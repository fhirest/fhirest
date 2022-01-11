CREATE OR REPLACE FUNCTION string(r resource, path text) RETURNS text AS $$
  WITH data as (SELECT unnest(jsonpath(r.content, path)) d),
       paths as (select (p->>'value')::text[] val FROM subpaths(r.type, path, 'string') p)
  SELECT '`' || string_agg(v#>>'{}', '`') || '`' FROM (
       SELECT unnest(jsonpath(d, val)) FROM data, paths
  ) v(v)
$$ LANGUAGE SQL IMMUTABLE COST 1001;