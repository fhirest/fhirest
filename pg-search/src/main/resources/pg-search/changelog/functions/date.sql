CREATE OR REPLACE FUNCTION date(r resource, path text) RETURNS tstzrange[] AS $$
  WITH data as (SELECT unnest(jsonpath(r.content, path)) d),
       paths as (select (p->>'start')::text[] lower, (p->>'end')::text[] upper FROM subpaths(r.type, path, 'date') p)
  SELECT array_agg(tstzrange('[' || coalesce(d#>>lower, 'infinity') || ',' || coalesce(d#>>upper, 'infinity') || ']'))
  FROM data, paths WHERE (d#>>lower) <= (coalesce(d#>>upper, d#>>lower))
$$ LANGUAGE SQL IMMUTABLE;