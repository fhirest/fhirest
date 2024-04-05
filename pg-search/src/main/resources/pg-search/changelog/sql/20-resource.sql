--liquibase formatted sql

--changeset fhirest:resource
--validCheckSum: 9:f38435ac06b1e4086338aa78a3cad2ec
create table search.resource (
  sid           bigserial,
  resource_type bigint references search.resource_type(id),
  resource_id   text not null,
  last_updated  timestamptz not null,
  active boolean default true
) PARTITION BY LIST (resource_type);
create index on search.resource(resource_id, resource_type);
--

--changeset fhirest:resource-drop-pk
alter table search.resource drop constraint if exists search_resource_pk;
--

