CREATE OR REPLACE FUNCTION reference(r resource, path text) RETURNS text[] AS $$
  WITH data as (SELECT unnest(jsonpath(r.content, path)) d),
       paths as (select (p->>'value')::text[] val FROM subpaths(r.type, path, 'reference') p),
       url_parts as (SELECT regexp_split_to_array(trim(both '/' from d#>>val), '/')::text[] parts FROM data, paths)
  SELECT array_agg(v) FROM (
       SELECT parts[2] FROM url_parts where array_length(parts, 1) > 1
       union all
       SELECT parts[1] || '/' || parts[2] FROM url_parts where array_length(parts, 1) > 1
       union all
       SELECT parts[1] || '/' || parts[2] || '/' || parts[3] || '/' || parts[4] FROM url_parts where array_length(parts, 1) > 3
  ) v(v)
$$ LANGUAGE SQL IMMUTABLE COST 1000;