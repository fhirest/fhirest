package ee.fhir.fhirest.scheduler.manage;

public class SchedulerJobException extends RuntimeException {
  public SchedulerJobException(String message) {
    super(message);
  }
}
