CREATE OR REPLACE FUNCTION number(r resource, path text) RETURNS numeric[] AS $$
  WITH data as (SELECT unnest(jsonpath(r.content, path)) d),
       paths as (select (p->>'value')::text[] val FROM subpaths(r.type, path, 'number') p)
  SELECT array_agg((d#>>val)::numeric)
  FROM data, paths WHERE (d#>>val) is not null
$$ LANGUAGE SQL IMMUTABLE;