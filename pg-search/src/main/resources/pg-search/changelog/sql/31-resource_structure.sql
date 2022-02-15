--liquibase formatted sql

--changeset kefhir:resource_structutre dbms:postgresql
drop table if exists search.resource_structure;
create table search.resource_structure (
  base          text not null,
  path 			text not null,
  element_type  text,
  is_many 		boolean not null
);
create index on search.resource_structure(base, path);
--rollback drop table resource_structure;

--changeset kefhir:resource_structure_recursive dbms:postgresql
drop MATERIALIZED view if exists search.resource_structure_recursive;
create materialized view search.resource_structure_recursive(base, path, element_type, is_many) as
WITH RECURSIVE struct(base, path, element_type, is_many) AS (
    SELECT resource_structure.* depth FROM search.resource_structure
  UNION ALL
    SELECT struct.base, struct.path || '.' || resource_structure.path, resource_structure.element_type, resource_structure.is_many
    FROM search.resource_structure, struct
    WHERE resource_structure.base = struct.element_type
    and struct.element_type not in ('Extension', 'Id', 'Coding', 'Identifier', 'string')
  )
SELECT * from struct;
create index on search.resource_structure_recursive(base, path);
--