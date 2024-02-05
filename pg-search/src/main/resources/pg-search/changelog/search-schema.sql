--liquibase formatted sql

--changeset fhirest:init-fhir-session-user runAlways:true
select core.set_user('"liquibase"'::jsonb);
--rollback select 1;

--changeset fhirest:create_schema_search
  create schema if not exists search;
--

--changeset fhirest:default_privileges_for_schema_search
  GRANT USAGE ON SCHEMA search TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT USAGE ON SEQUENCES TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT EXECUTE ON FUNCTIONS TO ${app-username};
  ALTER DEFAULT PRIVILEGES IN SCHEMA search GRANT SELECT,INSERT,UPDATE ON TABLES TO ${app-username};
--
