CREATE OR REPLACE FUNCTION search.extract_number(_type text, _content jsonb, _path text)
RETURNS TABLE (range numrange)
AS $$
  WITH paths as (select p.element, (p.path->>'lower')::text[] lower, (p.path->>'upper')::text[] upper FROM search.subpaths(_type, _path, 'number') p),
       data  as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths)
  SELECT numrange((data.d#>>lower)::numeric, (data.d#>>upper)::numeric, '[]')
   FROM data WHERE data.d is not null
$$ LANGUAGE SQL IMMUTABLE;
