--liquibase formatted sql

--changeset kefhir:base_index_string dbms:postgresql
CREATE TABLE search.base_index_string (
  sid bigint, -- references search.resource(sid),
  blindex_id bigint references search.blindex(id),
  active boolean default true,
  string text
) PARTITION BY LIST (blindex_id);
CREATE INDEX ON search.base_index_string (sid, string) where active = true;
--

--changeset kefhir:base_index_token dbms:postgresql
CREATE TABLE search.base_index_token (
  sid bigint, -- references search.resource(sid),
  blindex_id bigint references search.blindex(id),
  active boolean default true,
  system_id bigint references search.system(id),
  value text
) PARTITION BY LIST (blindex_id);
CREATE INDEX ON search.base_index_token (sid, system_id, value) where active = true;
--

--changeset kefhir:base_index_reference dbms:postgresql
CREATE TABLE search.base_index_reference (
  sid bigint, -- references search.resource(sid),
  blindex_id bigint references search.blindex(id),
  active boolean default true,
  type_id bigint references search.resource_type(id),
  id text
) PARTITION BY LIST (blindex_id);
CREATE INDEX ON search.base_index_reference (sid, type_id, id) where active = true;
--

--changeset kefhir:base_index_date dbms:postgresql
CREATE TABLE search.base_index_date (
  sid bigint, -- references search.resource(sid),
  blindex_id bigint references search.blindex(id),
  active boolean default true,
  range tstzrange
) PARTITION BY LIST (blindex_id);
CREATE INDEX ON search.base_index_date using gist (sid, range) where active = true;
--



