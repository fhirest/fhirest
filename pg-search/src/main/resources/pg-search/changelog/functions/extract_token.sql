CREATE OR REPLACE FUNCTION search.extract_token(_type text, _content jsonb, _path text)
RETURNS TABLE(system text, value text)
AS $$
  WITH data as (SELECT unnest(search.jsonpath(_content, _path)) d),
       paths as (select (p->>'value')::text[] val_path, (p->>'namespace')::text[] ns_path, p->>'elements' elements_path FROM search.subpaths(_type, _path, 'token') p),
       subdata as (select unnest(search.jsonpath(d, elements_path)) d, val_path, ns_path from data, paths)
  SELECT d#>>ns_path as system, d#>>val_path as value FROM subdata where d#>>val_path is not null
$$ LANGUAGE SQL IMMUTABLE COST 1000;
