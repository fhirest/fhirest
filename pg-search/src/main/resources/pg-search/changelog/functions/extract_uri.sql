CREATE OR REPLACE FUNCTION search.extract_uri(_type text, _content jsonb, _path text)
RETURNS TABLE(uri text)
AS $$
  WITH paths as (select p.element, (p.path->>'value')::text[] val_path FROM search.subpaths(_type, _path, 'uri') p),
       data as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths),
       values as (SELECT unnest(search.jsonpath(data.d, val_path)) val FROM data where data.d is not null)
  SELECT val#>>'{}' FROM values where val#>>'{}' is not null
$$ LANGUAGE SQL IMMUTABLE COST 1001;
