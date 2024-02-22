package ee.fhir.fhirest;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;

public final class SpringLiquibaseBuilder {
  public static SpringLiquibase build(DataSource ds, LiquibaseProperties p) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(ds);
    liquibase.setChangeLog(p.getChangeLog());
    liquibase.setContexts(p.getContexts());
    liquibase.setDefaultSchema(p.getDefaultSchema());
    liquibase.setDropFirst(p.isDropFirst());
    liquibase.setShouldRun(p.isEnabled());
    liquibase.setChangeLogParameters(p.getParameters());
    liquibase.setRollbackFile(p.getRollbackFile());
    return liquibase;
  }
}
