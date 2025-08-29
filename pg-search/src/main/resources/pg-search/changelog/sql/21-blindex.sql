--liquibase formatted sql

--changeset fhirest:blindex dbms:postgresql
create table search.blindex (
  id			bigserial primary key,
  resource_type text not null,
  path 			text not null,
  param_type 	text not null,
  index_name 	text not null,
  constraint blindex_ukey unique (resource_type,path,param_type)
);
--rollback drop table blindex;

--changeset fhirest:blindex-notifier
select core.add_object_notifier('search.blindex');
--

--changeset fhirest:blindex-add-fhirpath
alter table search.blindex add column fhirpath boolean default false not null;
--
