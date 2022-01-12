CREATE OR REPLACE FUNCTION search.number(r store.resource, path text) RETURNS numeric[] AS $$
  WITH data as (SELECT unnest(search.jsonpath(r.content, path)) d),
       paths as (select (p->>'value')::text[] val FROM search.subpaths(r.type, path, 'number') p)
  SELECT array_agg((d#>>val)::numeric)
  FROM data, paths WHERE (d#>>val) is not null
$$ LANGUAGE SQL IMMUTABLE;
