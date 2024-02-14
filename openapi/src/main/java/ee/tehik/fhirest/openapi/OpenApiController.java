package ee.tehik.fhirest.openapi;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("fhir-swagger")
@RequiredArgsConstructor
public class OpenApiController {
  private final OpenapiComposer composer;

  @GetMapping(produces = "application/yaml")
  public ResponseEntity<String> swag() {
    return ResponseEntity.of(Optional.of(composer.generateOpenApiYaml()));
  }


}
