--liquibase formatted sql

--changeset kefhir:search_init
select store.define_resource('CapabilityStatement');
select store.define_resource('StructureDefinition');
select store.define_resource('SearchParameter');
select store.define_resource('OperationDefinition');
select store.define_resource('CompartmentDefinition');
select store.define_resource('ValueSet');
select store.define_resource('CodeSystem');
select store.define_resource('ConceptMap');
--

