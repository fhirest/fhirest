package ee.tehik.fhirest.openapi;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import lombok.RequiredArgsConstructor;

@Controller("${fhirest.swagger.path:/fhir-swagger}")
@RequiredArgsConstructor
public class OpenApiController {
  private final OpenapiComposer composer;

  @Get
  public String swag() {
    return composer.generateOpenApiYaml();
  }


}
