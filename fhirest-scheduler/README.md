# Scheduler module
Scheduler module allows to schedule tasks and action on a resource in some specific time in the future.
It is required to implement tasks and schedule them manually.
Every scheduled task will be executed single time.


## Usage
```
implementation "ee.fhir.fhirest:fhirest-scheduler:${fhirestVersion}"
```
Scheduler module will use [pg-core](../pg-core) defined datasources 
You can also define separate datasource and liquibase configurations with prefix `scheduler-app`
```
spring:
  datasource:
    scheduler-app:
      url: jdbc:postgresql://localhost:5151/fhirest
      username: fhirest_scheduler
      password: test
      maxActive: 1
      driver.class.name: org.postgresql.Driver
      liquibase:
        change-log: 'classpath:changelog.xml'
        parameters:
          app-username: ${spring.datasource.scheduler-app.username}
```

For every task type you need an implementation
```
interface ScheduleJobRunner {
  String getType();
  String run(String identifier);
}
```
where
* `getType` is a unique task name
* `identifier` is a unique key provided during task scheduling.
* `run` return String may be any informational text and will be stored in the database


To schedule a task, one must use `SchedulerService`.
```
class SchedulerService {
  void schedule(String type, String identifier, Date scheduled);
  void reschedule(String type, String identifier, Date scheduled);
  void unschedule(String type, String identifier)
}
```
Identifier may be any unique string, usually it is a resource id.  


## Examples
For example, every time you save a `MedicationRequest`, you want to make sure it will expire after some time by changing its status.
First, you will need to implement this job:
```
@Component
public class MedicationExpireJob implements ScheduleJobRunner {
  @Inject
  private ResourceService resourceService;
  @Inject
  private ResourceFormatService resourceFormatService;

  @Override
  public String getType() {
    return "medication-expire";
  }

  @Override
  public String run(String identifier) {
    ResourceId id = ResourceUtil.parseReference(identifier);
    MedicationRequest mr = resourceFormatService.parse(resourceService.load(id).getContent());
    if (mr.getStatus() != MedicationRequestStatus.ACTIVE) {
      // medication status already changed, no need to cancel
      return "medication request unchanged";
    }
    mr.setStatus(MedicationRequestStatus.STOPPED);
    resourceService.save(id, resourceFormatService.compose(mr, "json"), InteractionType.UPDATE);
    return "medication request cancelled";
  }

}
```
Now after MedicationRequest is saved, we need to tell scheduler to remember it. We can take advantage of [ResourceAfterSaveInterceptor](/todo/link) to do so.
```
public class MedicationExpireSchedulePlugin extends ResourceAfterSaveInterceptor {
  @Inject
  private ResourceFormatService formatService;
  @Inject
  private SchedulerService schedulerService;

  public MedicationExpireSchedulePlugin() {
    super(ResourceAfterSaveInterceptor.FINALIZATION);
  }

  @Override
  public void handle(ResourceVersion version) {
    if (!version.getId().getResourceType().equals(ResourceType.MedicationRequest.name())) {
      return;
    }
    MedicationRequest request = formatService.parse(version.getContent());
    String reference = ResourceType.MedicationRequest.name() + "/" + request.getId();
    if (request.getStatus() == MedicationRequestStatus.ACTIVE) {
      schedulerService.reschedule("medication-expire", reference, DateUtils.addHours(new Date(), 72));
    } else {
      schedulerService.unschedule("medication-expire", reference);
    }
  }

}
```
And that's all. Now, after every MedicationRequest save or update scheduler be ready to cancel it right after 72 hours passes.

