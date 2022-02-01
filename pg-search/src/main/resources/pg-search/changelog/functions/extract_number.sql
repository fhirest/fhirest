CREATE OR REPLACE FUNCTION search.extract_number(_type text, _content jsonb, _path text)
RETURNS TABLE (number numeric)
AS $$
  WITH data as (SELECT unnest(search.jsonpath(_content, _path)) d),
       paths as (select (p->>'value')::text[] val_path FROM search.subpaths(_type, _path, 'number') p)
  SELECT (d#>>val_path)::numeric FROM data, paths WHERE (d#>>val_path) is not null
$$ LANGUAGE SQL IMMUTABLE;
