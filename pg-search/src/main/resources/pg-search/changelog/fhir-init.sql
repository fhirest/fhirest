--liquibase formatted sql

--changeset igor.bossenko@gmail.com:init-fhir-session-search_path runAlways:true
SET search_path TO fhir,core,public;
--rollback select 1;

--changeset igor.bossenko@gmail.com:init-fhir-session-user runAlways:true
select set_user('"admin"'::jsonb);
--rollback select 1;
