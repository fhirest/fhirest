CREATE OR REPLACE FUNCTION search.extract_number(_type text, _content jsonb, _path text)
RETURNS TABLE (number numeric)
AS $$
  WITH paths as (select p.element, (p.path->>'value')::text[] val_path FROM search.subpaths(_type, _path, 'number') p),
       data  as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths)
  SELECT (data.d#>>val_path)::numeric FROM data WHERE data.d is not null and (data.d#>>val_path) is not null
$$ LANGUAGE SQL IMMUTABLE;
