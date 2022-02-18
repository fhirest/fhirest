CREATE OR REPLACE FUNCTION search.extract_token(_type text, _content jsonb, _path text)
RETURNS TABLE(system text, value text)
AS $$
  WITH paths as (select p.element, (p.path->>'value')::text[] val_path, (p.path->>'namespace')::text[] ns_path, p.path->>'elements' elements_path FROM search.subpaths(_type, _path, 'token') p),
       data as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths),
       subdata as (select unnest(search.jsonpath(data.d, elements_path)) d, val_path, ns_path from data where data.d is not null)
  SELECT d#>>ns_path as system, d#>>val_path as value FROM subdata where d#>>val_path is not null
$$ LANGUAGE SQL IMMUTABLE COST 1000;
