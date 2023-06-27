package com.kodality.kefhir.openapi;

import ca.uhn.fhir.rest.api.Constants;
import com.kodality.kefhir.core.api.conformance.ConformanceUpdateListener;
import com.kodality.kefhir.core.model.InteractionType;
import com.kodality.kefhir.core.service.conformance.ConformanceHolder;
import com.kodality.kefhir.core.service.conformance.HapiContextHolder;
import com.kodality.kefhir.core.util.BeanContext;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r5.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.OperationDefinition;
import org.hl7.fhir.r5.model.OperationDefinition.OperationDefinitionParameterComponent;
import org.hl7.fhir.r5.model.OperationDefinition.OperationParameterScope;

@Singleton
@RequiredArgsConstructor
public class OpenapiComposer implements ConformanceUpdateListener {
  private final HapiContextHolder hapiContextHolder;
  private final BeanContext bc; //needed for static ref
  private static final String ROOT = "root";
  private String openApiYaml;

  @Override
  public void updated() {
    this.openApiYaml = Yaml.pretty(generateOpenApi());
  }

  public String generateOpenApiYaml() {
    return this.openApiYaml;
  }

  private OpenAPI generateOpenApi() {
    CapabilityStatement cs = ConformanceHolder.getCapabilityStatement();
    if (cs == null) {
      return null;
    }

    OpenAPI openApi = new OpenAPI();

    openApi.setInfo(new Info()
        .description(cs.getDescription())
        .title(cs.getSoftware().getName())
        .version(cs.getSoftware().getVersion())
        .contact(new Contact()
            .name(cs.getContactFirstRep().getName())
            .email(cs.getContactFirstRep().getTelecom().stream().filter(t -> t.getSystem() == ContactPointSystem.EMAIL)
                .map(ContactPoint::getValue).findFirst().orElse(null))
        )
    );

    openApi.setComponents(new Components()
        .addSchemas("fhir_Resource", new ObjectSchema().description("FHIR Resource"))
    );

    openApi.addServersItem(new Server()
        .url(cs.getImplementation().getUrl())
        .description(cs.getSoftware().getName())
    );

    openApi.addTagsItem(new Tag()
        .name(ROOT)
        .description("Whole System Interactions")
    );


    Paths paths = new Paths();
    openApi.setPaths(paths);

    registerRootInteraction(paths, InteractionType.CONFORMANCE);
    cs.getRestFirstRep().getInteraction().forEach(i -> registerRootInteraction(paths, i.getCode().toCode()));
    cs.getRestFirstRep().getOperation().forEach(op -> registerRootOperation(paths, op));

    cs.getRestFirstRep().getResource().forEach(resource -> {
      String resourceType = resource.getType();
      openApi.addTagsItem(new Tag()
          .name(resourceType)
          .description(resourceType + " Resource Interactions")
      );
      resource.getInteraction().forEach(i -> registerResourceInteraction(paths, resourceType, i.getCode().toCode(), resource));
      resource.getOperation().forEach(opComponent -> registerResourceOperation(paths, resourceType, opComponent));
    });
    return openApi;
  }

  private void registerRootInteraction(Paths paths, String interaction) {
    switch (interaction) {
      case InteractionType.CONFORMANCE -> addOperation(paths, "/metadata", "GET")
          .addTagsItem(ROOT).summary(InteractionType.CONFORMANCE)
          .responses(resourceResponse("200", "CapabilityStatement"));
      case InteractionType.TRANSACTION -> addOperation(paths, "/", "POST")
          .addTagsItem(ROOT).summary(InteractionType.TRANSACTION)
          .requestBody(resourceRequest("Bundle"))
          .responses(resourceResponse("200", "Bundle"));
      case InteractionType.HISTORYSYSTEM -> addOperation(paths, "/_history", "GET")
          .addTagsItem(ROOT).summary(InteractionType.HISTORYSYSTEM)
          .responses(resourceResponse("200", "Bundle"));
    }
  }

  private void registerRootOperation(Paths paths, CapabilityStatementRestResourceOperationComponent opComponent) {
    Stream.of(
        addOperation(paths, "/$" + opComponent.getName(), "GET"),
        addOperation(paths, "/$" + opComponent.getName(), "POST")
            .requestBody(resourceRequest("Parameters"))
    ).forEach(op -> op
        .addTagsItem(ROOT).summary(InteractionType.OPERATION).description(opComponent.getName())
        .responses(resourceResponse("200", null))
    );
  }

  private void registerResourceInteraction(Paths paths, String resourceType, String interaction, CapabilityStatementRestResourceComponent resource) {
    switch (interaction) {
      case InteractionType.READ -> addOperation(paths, "/" + resourceType + "/{id}", "GET")
          .addTagsItem(resourceType).summary(InteractionType.READ)
          .addParametersItem(resourceIdParameter())
          .responses(resourceResponse("200", resourceType));
      case InteractionType.VREAD -> addOperation(paths, "/" + resourceType + "/{id}/_history/{vid}", "GET")
          .addTagsItem(resourceType).summary(InteractionType.VREAD)
          .addParametersItem(resourceIdParameter())
          .addParametersItem(resourceVersionIdParameter())
          .responses(resourceResponse("200", resourceType));
      case InteractionType.CREATE -> addOperation(paths, "/" + resourceType, "POST")
          .addTagsItem(resourceType).summary(InteractionType.CREATE)
          .requestBody(resourceRequest(resourceType))
          .responses(resourceResponse("201", null));
      case InteractionType.UPDATE -> addOperation(paths, "/" + resourceType + "/{id}", "PUT")
          .addTagsItem(resourceType).summary(InteractionType.UPDATE)
          .addParametersItem(resourceIdParameter())
          .requestBody(resourceRequest(resourceType))
          .responses(resourceResponse("200", null));
      case InteractionType.HISTORYTYPE -> addOperation(paths, "/" + resourceType + "/_history", "GET")
          .addTagsItem(resourceType).summary(InteractionType.HISTORYTYPE)
          .responses(resourceResponse("200", "Bundle"));
      case InteractionType.HISTORYINSTANCE -> addOperation(paths, "/" + resourceType + "/{id}/_history", "GET")
          .addTagsItem(resourceType).summary(InteractionType.HISTORYINSTANCE)
          .addParametersItem(resourceIdParameter())
          .responses(resourceResponse("200", "Bundle"));
      case InteractionType.DELETE -> addOperation(paths, "/" + resourceType + "/{id}", "DELETE")
          .addTagsItem(resourceType).summary(InteractionType.DELETE)
          .addParametersItem(resourceIdParameter())
          .responses(resourceResponse("204", null));
      case InteractionType.SEARCHTYPE -> Stream.of(
          addOperation(paths, "/" + resourceType, "GET"),
          addOperation(paths, "/" + resourceType + "/_search", "POST")
      ).forEach(op -> op
              .addTagsItem(resourceType).summary(InteractionType.SEARCHTYPE)
              .responses(resourceResponse("200", "Bundle"))
              .setParameters(resource.getSearchParam().stream().map(sp -> new Parameter()
                      .name(sp.getName())
                      .in("query")
                      .description(sp.getDocumentation())
                      .schema(new Schema().type("string"))
//              .style(Parameter.StyleEnum.SIMPLE)
              ).toList())
      );
    }
  }

  private void registerResourceOperation(Paths paths, String resourceType, CapabilityStatementRestResourceOperationComponent opComponent) {
    OperationDefinition opDef = ConformanceHolder.getOperationDefinition(opComponent.getDefinition());
    if (opDef == null) {
      return;
    }
    if (opDef.getType()) {
      addOperation(paths, "/" + resourceType + "/$" + opComponent.getName(), "POST")
          .requestBody(resourceRequest("Parameters"))
          .addTagsItem(resourceType)
          .summary(InteractionType.OPERATION)
          .description(opComponent.getName())
          .responses(resourceResponse("200", null));
      addOperation(paths, "/" + resourceType + "/$" + opComponent.getName(), "GET")
          .parameters(opDef.getParameter().stream()
              .filter(p -> p.getScope() == null || p.getScope().stream().anyMatch(ps -> ps.getCode().equals(OperationParameterScope.TYPE.toCode())))
              .map(this::operationParameter)
              .collect(Collectors.toList()))
          .addTagsItem(resourceType)
          .summary(InteractionType.OPERATION)
          .description(opComponent.getName())
          .responses(resourceResponse("200", null));
    }
    if (opDef.getInstance()) {
      addOperation(paths, "/" + resourceType + "/{id}/$" + opComponent.getName(), "POST")
          .addParametersItem(resourceIdParameter())
          .requestBody(resourceRequest("Parameters"))
          .addTagsItem(resourceType)
          .summary(InteractionType.OPERATION)
          .description(opComponent.getName())
          .responses(resourceResponse("200", null));
      addOperation(paths, "/" + resourceType + "/{id}/$" + opComponent.getName(), "GET")
          .parameters(opDef.getParameter().stream()
              .filter(p -> p.getScope() == null || p.getScope().stream().anyMatch(ps -> ps.getCode().equals(OperationParameterScope.INSTANCE.toCode())))
              .map(this::operationParameter)
              .collect(Collectors.toList()))
          .addParametersItem(resourceIdParameter())
          .addTagsItem(resourceType)
          .summary(InteractionType.OPERATION)
          .description(opComponent.getName())
          .responses(resourceResponse("200", null));
    }
  }

  protected Operation addOperation(Paths apiPaths, String path, String method) {
    PathItem pathItem = apiPaths.computeIfAbsent(path, k -> new PathItem());

    Operation op = new Operation();
    switch (method) {
      case "POST":
        assert pathItem.getPost() == null;
        return pathItem.post(op).getPost();
      case "GET":
        assert pathItem.getGet() == null;
        return pathItem.get(op).getGet();
      case "PUT":
        assert pathItem.getPut() == null;
        return pathItem.put(op).getPut();
      case "PATCH":
        assert pathItem.getPatch() == null;
        return pathItem.patch(op).getPatch();
      case "DELETE":
        assert pathItem.getDelete() == null;
        return pathItem.delete(op).getDelete();
      default:
        throw new IllegalStateException("unsupported method " + method);
    }
  }

  private RequestBody resourceRequest(String resourceType) {
    return new RequestBody().content(buildResourceApiContent(resourceType));
  }

  private ApiResponses resourceResponse(String name, String resourceType) {
    return new ApiResponses().addApiResponse(name, new ApiResponse()
        .content(resourceType == null ? null : buildResourceApiContent(resourceType))
        .description(resourceType == null ? "response" : ("resource '" + resourceType + "'"))
    );
  }

  private Content buildResourceApiContent(String resourceType) {
    IBaseResource example = hapiContextHolder.getContext().getResourceDefinition(resourceType).newInstance();
    String exampleJson = hapiContextHolder.getContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(example);

    Content result = new Content();
    MediaType jsonSchema = new MediaType().schema(new ObjectSchema().$ref("#/components/schemas/fhir_Resource"));
    jsonSchema.setExample(exampleJson);
    result.addMediaType(Constants.CT_FHIR_JSON_NEW, jsonSchema);
    return result;
  }

  private Parameter operationParameter(OperationDefinitionParameterComponent defParameter) {
    return new Parameter().name(defParameter.getName()).in("query").description(defParameter.getDocumentation())
        .schema(new Schema<>().type("string"));
  }

  private Parameter resourceIdParameter() {
    return new Parameter().name("id").in("path").description("Resource Id").example("123")
        .schema(new Schema<>().type("string").minimum(new BigDecimal(1))).style(Parameter.StyleEnum.SIMPLE);
  }

  private Parameter resourceVersionIdParameter() {
    return new Parameter().name("vid").in("path").description("Resource Version Id").example("1")
        .schema(new Schema<>().type("string").minimum(new BigDecimal(1))).style(Parameter.StyleEnum.SIMPLE);
  }

}
