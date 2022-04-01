--liquibase formatted sql

--changeset kefhir:system dbms:postgresql
create table search.system (
  id     bigserial primary key,
  system text,
  constraint system_ukey unique (system)
);
--

