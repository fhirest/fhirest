--liquibase formatted sql

--changeset kefhir:resource dbms:postgresql
CREATE SEQUENCE store.resource_key_seq INCREMENT 1 MINVALUE 1;

create table store.resource (
  uid           bigint not null default nextval('store.resource_key_seq'),
  type          text not null,
  id            text not null,
  version       smallint not null default 1,
  updated       timestamptz not null default now(),
  author        jsonb,
  content       jsonb,
  sys_status    char(1) not null default 'A',
  constraint resource_id_not_empty check (id != '')
) PARTITION BY LIST (type);
--


--changeset kefhir:resource_key_seq  dbms:postgresql
CREATE SEQUENCE store.resource_id_seq INCREMENT 1 MINVALUE 1;
SELECT setval('store.resource_id_seq', nextval('store.resource_key_seq'));
--rollback select 1

--changeset kefhir:resource_updated_index
create index on store.resource(updated);
--
