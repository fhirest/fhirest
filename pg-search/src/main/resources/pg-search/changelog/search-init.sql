--liquibase formatted sql

--changeset kefhir:init-fhir-session-user runAlways:true
select core.set_user('"liquibase"'::jsonb);
--rollback select 1;
