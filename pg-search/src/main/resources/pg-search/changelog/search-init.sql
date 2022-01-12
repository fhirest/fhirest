--liquibase formatted sql

--changeset igor.bossenko@gmail.com:init-fhir-session-user runAlways:true
select core.set_user('"liquibase"'::jsonb);
--rollback select 1;
