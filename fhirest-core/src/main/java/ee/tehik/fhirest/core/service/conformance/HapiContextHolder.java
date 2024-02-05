package ee.tehik.fhirest.core.service.conformance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.hapi.converters.canonical.VersionCanonicalizer;
import ee.tehik.fhirest.core.api.conformance.ConformanceUpdateListener;
import ee.tehik.fhirest.core.api.conformance.HapiValidationSupportProvider;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.common.hapi.validation.validator.ProfileKnowledgeWorkerR5;
import org.hl7.fhir.common.hapi.validation.validator.VersionSpecificWorkerContextWrapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.conformance.profile.ProfileKnowledgeProvider;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r5.model.Enumerations.CodeSystemContentMode;
import org.hl7.fhir.r5.model.PackageInformation;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService.getFhirVersionEnum;

@RequiredArgsConstructor
@Singleton
public class HapiContextHolder implements ConformanceUpdateListener {

  public static final List<CodeSystemContentMode> SUPPORTED_CS_CONTENT_MODES = Arrays.asList(CodeSystemContentMode.COMPLETE, CodeSystemContentMode.FRAGMENT);

  protected IWorkerContext hapiContext;
  protected FhirContext context;
  protected FhirValidator validator;
  protected final List<HapiValidationSupportProvider> validationSupportProviders;

  public IWorkerContext getHapiContext() {
    return hapiContext;
  }

  public FhirContext getContext() {
    return context;
  }

  public FhirValidator getValidator() {
    return validator;
  }

  @PostConstruct
  public void init() {
    context = FhirContext.forR5();
  }

  @Override
  public void updated() {
    IValidationSupport validationSupport = getValidationSupport();

    hapiContext = new HapiWorkerContext(context, validationSupport);
    context.setValidationSupport(validationSupport);

    validator = context.newValidator();
    validator.registerValidatorModule(new FhirInstanceValidator(validationSupport));
    preloadHapi();
  }

  protected IValidationSupport getValidationSupport() {
    Map<String, IBaseResource> defs = ConformanceHolder.getDefinitions().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d, (a, b) -> a));
    // hack to bypass hapi structuredefinition.type validation, (#see org.hl7.fhir.validation.instance.InstanceValidator.checkTypeValue)
    // because wo don't have information about "source package" and it isn't even a part of fhir resource.
    defs.values().forEach(def -> ((Resource) def).setSourcePackage(new PackageInformation("hl7.fhir.r", def.getMeta().getLastUpdated())));
    Map<String, IBaseResource> vs = ConformanceHolder.getValueSets().stream().collect(Collectors.toMap(d -> d.getUrl(), d -> d, (a, b) -> a));
    Map<String, IBaseResource> cs = ConformanceHolder.getCodeSystems().stream()
        .filter(c -> SUPPORTED_CS_CONTENT_MODES.contains(c.getContent()))
        .collect(Collectors.toMap(d -> d.getUrl(), d -> d, (a, b) -> a));

    ValidationSupportChain chain = new ValidationSupportChain(
        new InMemoryTerminologyServerValidationSupport(context),
        new PrePopulatedValidationSupport(context, defs, vs, cs),
        new CommonCodeSystemsTerminologyService(context),
        new FhirestSnapshotGeneratingValidationSupport(context)
    );
    validationSupportProviders.forEach(p -> chain.addValidationSupport(p.getValidationSupport(context)));
    return new CachingValidationSupport(chain);
  }

  private void preloadHapi() {
    try {
      validator.validateWithResult("{}");
    } catch (Exception e) {
      //ignore
    }
  }

  /**
   * should use SnapshotGeneratingValidationSupport from hapi.
   * fixes issue https://github.com/hapifhir/hapi-fhir/issues/4978
   * delete this when resolved
   */
  private static class FhirestSnapshotGeneratingValidationSupport implements IValidationSupport {
    private static final Logger ourLog = LoggerFactory.getLogger(org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport.class);
    private final FhirContext myCtx;
    private final VersionCanonicalizer myVersionCanonicalizer;

    /**
     * Constructor
     */
    public FhirestSnapshotGeneratingValidationSupport(FhirContext theCtx) {
      myCtx = Objects.requireNonNull(theCtx);
      myVersionCanonicalizer = new VersionCanonicalizer(theCtx);
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    @Override
    public IBaseResource generateSnapshot(ValidationSupportContext theValidationSupportContext, IBaseResource theInput, String theUrl, String theWebUrl, String theProfileName) {

      String inputUrl = null;
      try {
        FhirVersionEnum version = theInput.getStructureFhirVersionEnum();

        org.hl7.fhir.r5.model.StructureDefinition inputCanonical = myVersionCanonicalizer.structureDefinitionToCanonical(theInput);

        inputUrl = inputCanonical.getUrl();
        if (theValidationSupportContext.getCurrentlyGeneratingSnapshots().contains(inputUrl)) {
          ourLog.warn("Detected circular dependency, already generating snapshot for: {}", inputUrl);
          return theInput;
        }
        theValidationSupportContext.getCurrentlyGeneratingSnapshots().add(inputUrl);

        String baseDefinition = inputCanonical.getBaseDefinition();
        if (isBlank(baseDefinition)) {
          throw new PreconditionFailedException(Msg.code(704) + "StructureDefinition[id=" + inputCanonical.getIdElement().getId() + ", url=" + inputCanonical.getUrl() + "] has no base");
        }

        IBaseResource base = theValidationSupportContext.getRootValidationSupport().fetchStructureDefinition(baseDefinition);
        if (base == null) {
          throw new PreconditionFailedException(Msg.code(705) + "Unknown base definition: " + baseDefinition);
        }

        org.hl7.fhir.r5.model.StructureDefinition baseCanonical = myVersionCanonicalizer.structureDefinitionToCanonical(base);

        if (baseCanonical.getSnapshot().getElement().isEmpty()) {
          // If the base definition also doesn't have a snapshot, generate that first
          theValidationSupportContext.getRootValidationSupport().generateSnapshot(theValidationSupportContext, base, null, null, null);
          baseCanonical = myVersionCanonicalizer.structureDefinitionToCanonical(base);
        }

        ArrayList<ValidationMessage> messages = new ArrayList<>();
        ProfileKnowledgeProvider profileKnowledgeProvider = new ProfileKnowledgeWorkerR5(myCtx);
        IWorkerContext context = new VersionSpecificWorkerContextWrapper(theValidationSupportContext, myVersionCanonicalizer);
        ProfileUtilities profileUtilities = new ProfileUtilities(context, messages, profileKnowledgeProvider);
        profileUtilities.generateSnapshot(baseCanonical, inputCanonical, theUrl, theWebUrl, theProfileName);

        if (inputCanonical == theInput) {
          return theInput;
        }

        switch (getFhirVersionEnum(theValidationSupportContext.getRootValidationSupport().getFhirContext(), theInput)) {
          case DSTU3:
            org.hl7.fhir.dstu3.model.StructureDefinition generatedDstu3 = (org.hl7.fhir.dstu3.model.StructureDefinition) myVersionCanonicalizer.structureDefinitionFromCanonical(inputCanonical);
            ((org.hl7.fhir.dstu3.model.StructureDefinition) theInput).getSnapshot().getElement().clear();
            ((org.hl7.fhir.dstu3.model.StructureDefinition) theInput).getSnapshot().getElement().addAll(generatedDstu3.getSnapshot().getElement());
            break;
          case R4:
            org.hl7.fhir.r4.model.StructureDefinition generatedR4 = (org.hl7.fhir.r4.model.StructureDefinition) myVersionCanonicalizer.structureDefinitionFromCanonical(inputCanonical);
            ((org.hl7.fhir.r4.model.StructureDefinition) theInput).getSnapshot().getElement().clear();
            ((org.hl7.fhir.r4.model.StructureDefinition) theInput).getSnapshot().getElement().addAll(generatedR4.getSnapshot().getElement());
            break;
          case R4B:
            org.hl7.fhir.r4b.model.StructureDefinition generatedR4b = (org.hl7.fhir.r4b.model.StructureDefinition) myVersionCanonicalizer.structureDefinitionFromCanonical(inputCanonical);
            ((org.hl7.fhir.r4b.model.StructureDefinition) theInput).getSnapshot().getElement().clear();
            ((org.hl7.fhir.r4b.model.StructureDefinition) theInput).getSnapshot().getElement().addAll(generatedR4b.getSnapshot().getElement());
            break;
          case R5:
            org.hl7.fhir.r5.model.StructureDefinition generatedR5 = (org.hl7.fhir.r5.model.StructureDefinition) myVersionCanonicalizer.structureDefinitionFromCanonical(inputCanonical);
            ((org.hl7.fhir.r5.model.StructureDefinition) theInput).getSnapshot().getElement().clear();
            ((org.hl7.fhir.r5.model.StructureDefinition) theInput).getSnapshot().getElement().addAll(generatedR5.getSnapshot().getElement());
            break;
          case DSTU2:
          case DSTU2_HL7ORG:
          case DSTU2_1:
          default:
            throw new IllegalStateException(Msg.code(706) + "Can not generate snapshot for version: " + version);
        }

        return theInput;

      } catch (BaseServerResponseException e) {
        throw e;
      } catch (Exception e) {
        throw new InternalErrorException(Msg.code(707) + "Failed to generate snapshot", e);
      } finally {
        if (inputUrl != null) {
          theValidationSupportContext.getCurrentlyGeneratingSnapshots().remove(inputUrl);
        }
      }
    }

    @Override
    public FhirContext getFhirContext() {
      return myCtx;
    }

  }

}
