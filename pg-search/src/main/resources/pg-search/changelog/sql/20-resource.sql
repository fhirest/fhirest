--liquibase formatted sql

--changeset kefhir:resource dbms:postgresql
create table search.resource (
  sid           bigserial,
  resource_type bigint references search.resource_type(id),
  resource_id   text not null,
  last_updated  timestamptz not null,
  active boolean default true,
  constraint search_resource_pk primary key (resource_type, sid)
) PARTITION BY LIST (resource_type);
create index on search.resource(resource_id, resource_type);
--

