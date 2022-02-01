CREATE OR REPLACE FUNCTION search.extract_date(_type text, _content jsonb, _path text)
RETURNS TABLE(range tstzrange)
AS $$
  WITH data as (SELECT unnest(search.jsonpath(_content, _path)) d),
       paths as (select (p->>'start')::text[] lower, (p->>'end')::text[] upper FROM search.subpaths(_type, _path, 'date') p)
  SELECT tstzrange('[' || coalesce(d#>>lower, 'infinity') || ',' || coalesce(d#>>upper, 'infinity') || ']')
  FROM data, paths WHERE (d#>>lower) <= (coalesce(d#>>upper, d#>>lower))
$$ LANGUAGE SQL IMMUTABLE;
