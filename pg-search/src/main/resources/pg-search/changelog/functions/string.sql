CREATE OR REPLACE FUNCTION search.string(r store.resource, path text) RETURNS text AS $$
  WITH data as (SELECT unnest(search.jsonpath(r.content, path)) d),
       paths as (select (p->>'value')::text[] val FROM search.subpaths(r.type, path, 'string') p)
  SELECT '`' || string_agg(v#>>'{}', '`') || '`' FROM (
       SELECT unnest(search.jsonpath(d, val)) FROM data, paths
  ) v(v)
$$ LANGUAGE SQL IMMUTABLE COST 1001;
