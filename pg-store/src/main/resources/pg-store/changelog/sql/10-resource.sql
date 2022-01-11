--liquibase formatted sql

--changeset kefhir:resource dbms:postgresql
CREATE SEQUENCE resource_key_seq INCREMENT 1 MINVALUE 1;

create table resource (
  key           bigint not null default nextval('resource_key_seq'),
  type          text not null,
  id            text not null,
  last_version  smallint not null default 1,
  last_updated  timestamp not null default localtimestamp,
  author        jsonb,
  content       jsonb not null,
  sys_status    char(1) not null default 'A'
) PARTITION BY LIST (type);
--rollback drop table resource cascade;

--changeset kefhir:resource-content-nullable dbms:postgresql
alter table resource alter column content DROP NOT NULL;
--rollback select 1

--changeset kefhir:resource-last_updated_timestamptz dbms:postgresql
alter table resource alter column last_updated type timestamptz
--rollback select 1

--changeset kefhir:resource_key_seq  dbms:postgresql
CREATE SEQUENCE resource_id_seq INCREMENT 1 MINVALUE 1;
SELECT setval('resource_id_seq', nextval('resource_key_seq'));
--rollback select 1
