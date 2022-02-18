CREATE OR REPLACE FUNCTION search.extract_date(_type text, _content jsonb, _path text)
RETURNS TABLE(range tstzrange)
AS $$
  with paths as (select p.element, (p.path->>'start')::text[] lower, (p.path->>'end')::text[] upper FROM search.subpaths(_type, _path, 'date') p),
       data as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths)
  SELECT tstzrange('[' || coalesce(data.d#>>lower, 'infinity') || ',' || coalesce(data.d#>>upper, 'infinity') || ']')
  FROM data where data.d is not null
$$ LANGUAGE SQL IMMUTABLE;
