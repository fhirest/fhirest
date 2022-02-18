--liquibase formatted sql

--changeset kefhir:search_configuration dbms:postgresql
create table search.search_configuration (
  id 			bigserial primary key,
  param_type 	text not null,
  element_type 	text not null,
  path 			jsonb not null,
  constraint search_configuration_element_type_param_type_ukey unique (element_type)
);
--rollback drop table search_configuration;

--changeset kefhir:search_configuration-data dbms:postgresql runOnChange:true
delete from search.search_configuration;

with t(param_type, element_type, path) as (values
  ('date', 'date',     '[{"start":"{}", "end":"{}"}]'),
  ('date', 'instant',  '[{"start":"{}", "end":"{}"}]'),
  ('date', 'dateTime', '[{"start":"{}", "end":"{}"}]'),
  ('date', 'Period',   '[{"start":"{start}", "end":"{end}"}]'),

  ('token', 'code',            '[{"value":"{}"}]'),
  ('token', 'CodeableConcept', '[{"elements":"coding", "namespace":"{system}", "value":"{code}"}]'),
  ('token', 'Coding',          '[{"namespace":"{system}", "value":"{code}"}]'),
  ('token', 'Identifier',      '[{"namespace":"{system}", "value":"{value}"}]'),
  ('token', 'ContactPoint',    '[{"namespace":"{system}", "value":"{value}"}]'),
  ('token', 'boolean',         '[{"value":"{}"}]'),

  ('string', 'string',    '[{"value":"{}"}]'),
  ('string', 'markdown',    '[{"value":"{}"}]'),
  ('string', 'HumanName', '[{"value":"{family}"}, {"value":"{given}"}]'),

  ('reference', 'Reference', '[{"value":"{reference}"}]'),

  ('number', 'integer', '[{"value":"{}"}]'),
  ('number', 'decimal', '[{"value":"{}"}]'),

  ('quantity', 'Quantity', '[{"value":"{value}", "system":"{system}", "code":"{code}", "unit":"{unit}"}]'),
  ('quantity', 'SampledData', '[{"value":"{lowerLimit}"}]') -- FIXME: temporary hack to avoid exceptions. used in Observations, however search spec claims it is not used
) insert into search.search_configuration(param_type, element_type, path) select t.param_type, t.element_type, t.path::jsonb from t;
--rollback delete from search_configuration;
