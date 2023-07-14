--liquibase formatted sql

--changeset kefhir:search_init-1
select search.define_resource('CapabilityStatement');
select search.define_resource('TerminologyCapabilities');
select search.define_resource('StructureDefinition');
select search.define_resource('SearchParameter');
select search.define_resource('OperationDefinition');
select search.define_resource('CompartmentDefinition');
select search.define_resource('ValueSet');
select search.define_resource('CodeSystem');
select search.define_resource('ConceptMap');
--

