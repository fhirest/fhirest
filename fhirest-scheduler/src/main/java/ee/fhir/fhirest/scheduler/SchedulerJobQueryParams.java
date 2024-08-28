package ee.fhir.fhirest.scheduler;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class SchedulerJobQueryParams {
  private Long id;
  private String status;
}
