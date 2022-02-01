--liquibase formatted sql

--changeset kefhir:blindex dbms:postgresql
create table search.blindex (
  id			bigserial primary key,
  resource_type text not null,
  path 			text not null,
  param_type 	text not null,
  index_name 	text not null,
  constraint blindex_ukey unique (resource_type,path)
);
--rollback drop table blindex;
