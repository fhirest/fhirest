package ee.fhir.fhirest.core.validation;

import ee.fhir.fhirest.core.api.resource.ResourceBeforeSaveInterceptor;
import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.InteractionType;
import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.structure.api.ResourceContent;
import ee.fhir.fhirest.structure.service.ResourceFormatService;
import jakarta.inject.Inject;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceIdValidator extends ResourceBeforeSaveInterceptor {
    @Inject
    private ResourceFormatService resourceFormatService;

    public ResourceIdValidator() {
        super(ResourceBeforeSaveInterceptor.INPUT_VALIDATION);
    }

    @Override
    public void handle(ResourceId id, ResourceContent content, String interaction) {
        Resource convertedResource = resourceFormatService.parse(content.getValue());
        if (interaction.equals(InteractionType.UPDATE) && (convertedResource.getId() == null || !id.getResourceId().equals(convertedResource.getId()))) {
            throw new FhirException(FhirestIssue.FEST_037);
        }
    }
}