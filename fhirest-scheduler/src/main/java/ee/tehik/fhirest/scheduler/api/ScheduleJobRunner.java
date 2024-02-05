package ee.tehik.fhirest.scheduler.api;

public interface ScheduleJobRunner {

  String getType();

  /**
   * @return just some log maybe
   */
  String run(String identifier);

}
