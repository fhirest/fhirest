--liquibase formatted sql

--changeset kefhir:init-fhir-session-user runAlways:true
select core.set_user('"liquibase"'::jsonb);
--rollback select 1;

--changeset kefhir:create_schema_scheduler
  create schema if not exists scheduler;
--

--changeset kefhir:default_privileges_for_schema_scheduler
  GRANT USAGE ON SCHEMA scheduler TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA scheduler GRANT USAGE ON SEQUENCES TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA scheduler GRANT EXECUTE ON FUNCTIONS TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA scheduler GRANT SELECT,INSERT,UPDATE ON TABLES TO ${app-username};
--
