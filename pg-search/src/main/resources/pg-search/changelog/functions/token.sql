CREATE OR REPLACE FUNCTION token(r resource, path text) RETURNS text[] AS $$
  WITH data as (SELECT unnest(jsonpath(r.content, path)) d),
       paths as (select (p->>'value')::text[] val, (p->>'namespace')::text[] ns, p->>'elements' elements FROM subpaths(r.type, path, 'token') p),
       subdata as (select unnest(jsonpath(d, elements)) d, val, ns from data, paths)
  SELECT array_agg(v) FROM (
       SELECT lower(d#>>val) FROM subdata
       union all
       SELECT COALESCE((lower(d#>>ns)), '') || '|' || (lower(d#>>val)) FROM subdata WHERE ns IS NOT NULL
  ) v(v)
$$ LANGUAGE SQL IMMUTABLE COST 1000;