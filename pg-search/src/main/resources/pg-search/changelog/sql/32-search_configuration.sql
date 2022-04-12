--liquibase formatted sql

--changeset kefhir:search_configuration dbms:postgresql
drop table if exists search.search_configuration;
create table search.search_configuration (
  id 			bigserial primary key,
  param_type 	text not null,
  element_type 	text not null,
  path 			jsonb not null
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
  ('token', 'string',          '[{"value":"{}"}]'),

  ('string', 'string',    '[{"value":"{}"}]'),
  ('string', 'markdown',    '[{"value":"{}"}]'),
  ('string', 'HumanName', '[{"value":"{family}"}, {"value":"{given}"}]'),

  ('reference', 'Reference', '[{"value":"{reference}"}]'),
  ('reference', 'Attachment', '[]'), -- do nothing

  ('number', 'integer', '[{"lower":"{}", "upper":"{}"}]'),
  ('number', 'decimal', '[{"lower":"{}", "upper":"{}"}]'),

  ('quantity', 'Quantity', '[{"lower":"{value}", "upper":"{value}", "system":"{system}", "code":"{code}", "unit":"{unit}"}]'),
  ('quantity', 'Age', '[{"lower":"{value}", "upper":"{value}", "system":"{system}", "code":"{code}", "unit":"{unit}"}]'),
  ('quantity', 'SampledData', '[{"lower":"{lowerLimit}", "upper":"{upperLimit}"}]'),
  ('quantity', 'Range', '[{"lower":"{low,value}", "upper":"{high,value}"}]'),

  ('uri', 'uri',    '[{"value":"{}"}]')
) insert into search.search_configuration(param_type, element_type, path) select t.param_type, t.element_type, t.path::jsonb from t;
--rollback delete from search_configuration;
