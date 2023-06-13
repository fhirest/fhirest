package com.kodality.kefhir.openapi;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import lombok.RequiredArgsConstructor;

@Controller("${kefhir.swagger.path:/fhir-swagger}")
@RequiredArgsConstructor
public class OpenApiController {
  private final OpenapiComposer composer;

  @Get
  public String swag() {
    return composer.generateOpenApiYaml();
  }


}
