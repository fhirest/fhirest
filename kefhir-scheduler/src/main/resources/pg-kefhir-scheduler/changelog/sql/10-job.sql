--liquibase formatted sql

--changeset kefhir:scheduler.job dbms:postgresql
create table scheduler.job (
  id bigserial primary key,
  type text not null,
  identifier text not null,
  scheduled timestamp not null,
  started timestamp,
  finished timestamp,
  log text,
  status text not null default 'active'
);
create index on scheduler.job(scheduled);
create index on scheduler.job(type, identifier);
--rollback drop table scheduler.job cascade;

