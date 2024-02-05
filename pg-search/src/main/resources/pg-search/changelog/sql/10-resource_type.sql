--liquibase formatted sql

--changeset fhirest:resource_type dbms:postgresql
create table search.resource_type (
  id   bigserial primary key,
  type text not null,
  constraint type_ukey unique (type)
);
--

