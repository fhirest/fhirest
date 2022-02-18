CREATE OR REPLACE FUNCTION search.extract_reference(_type text, _content jsonb, _path text)
RETURNS TABLE(type text, id text)
AS $$
  WITH paths as (select p.element, (p.path->>'value')::text[] val_path FROM search.subpaths(_type, _path, 'reference') p),
       data as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths),
       url_parts as (SELECT regexp_split_to_array(trim(both '/' from data.d#>>val_path), '/')::text[] parts FROM data where data.d is not null and data.d#>>val_path is not null)
  SELECT parts[1] as type, parts[2] as id FROM url_parts
$$ LANGUAGE SQL IMMUTABLE COST 1000;
