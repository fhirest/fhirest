CREATE OR REPLACE FUNCTION search.extract_reference(_type text, _content jsonb, _path text)
RETURNS TABLE(type text, id text)
AS $$
  WITH data as (SELECT unnest(search.jsonpath(_content, _path)) d),
       paths as (select (p->>'value')::text[] val_path FROM search.subpaths(_type, _path, 'reference') p),
       url_parts as (SELECT regexp_split_to_array(trim(both '/' from d#>>val_path), '/')::text[] parts FROM data, paths where d#>>val_path is not null)
  SELECT parts[1] as type, parts[2] as id FROM url_parts
$$ LANGUAGE SQL IMMUTABLE COST 1000;
