--liquibase formatted sql

--changeset kefhir:init-fhir-session-user runAlways:true
select core.set_user('"liquibase"'::jsonb);
--rollback select 1;

--changeset kefhir:create_schema_store
create schema if not exists store;
--

--changeset kefhir:default_privileges_for_schema_store
  GRANT USAGE ON SCHEMA store TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA store GRANT USAGE ON SEQUENCES TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA store GRANT EXECUTE ON FUNCTIONS TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA store GRANT SELECT,INSERT,UPDATE ON TABLES TO ${app-username};
--
