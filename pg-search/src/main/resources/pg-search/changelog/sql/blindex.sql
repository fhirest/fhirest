--liquibase formatted sql

--changeset kephir:blindex dbms:postgresql
create table blindex (
  id			bigserial primary key,
  resource_type text not null,
  path 			text not null,
  param_type 	text not null,
  index_type 	text not null,
  index_name 	text not null,
  constraint blindex_ukey unique (resource_type,path,param_type)
);
--rollback drop table blindex;
