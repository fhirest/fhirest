--liquibase formatted sql

--changeset kefhir:resource_structutre dbms:postgresql
drop table if exists search.resource_structure;
create table search.resource_structure (
  parent        text not null,
  child			text not null,
  alias         text not null,
  element_type  text,
  constraint resource_structure_ukey unique (parent, child)
);
create index on search.resource_structure(parent, child);
create index on search.resource_structure(parent, alias);
--rollback drop table resource_structure;

--changeset kefhir:resource_structure_recursive dbms:postgresql runOnChange:true
drop MATERIALIZED view if exists search.resource_structure_recursive;
create materialized view search.resource_structure_recursive(parent, alias, path, element_type) as
WITH RECURSIVE struct(parent, alias, path, element_type) AS (
    SELECT rs.parent, rs.alias, rs.child, rs.element_type FROM search.resource_structure rs
  UNION ALL
    SELECT struct.parent, null, struct.path || '.' || rs.child, rs.element_type
    FROM search.resource_structure rs, struct
    WHERE rs.parent = struct.element_type
    and struct.element_type not in ('Extension', 'Id', 'Coding', 'Identifier', 'string')
  )
SELECT * from struct;
create index on search.resource_structure_recursive(parent, path);
--
