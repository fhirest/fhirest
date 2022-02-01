CREATE OR REPLACE FUNCTION search.extract_string(_type text, _content jsonb, _path text)
RETURNS TABLE(string text)
AS $$
  WITH data as (SELECT unnest(search.jsonpath(_content, _path)) d),
       paths as (select (p->>'value')::text[] val_path FROM search.subpaths(_type, _path, 'string') p),
       values as (SELECT unnest(search.jsonpath(d, val_path)) val FROM data, paths)
  SELECT val#>>'{}' FROM values where val#>>'{}' is not null
$$ LANGUAGE SQL IMMUTABLE COST 1001;
