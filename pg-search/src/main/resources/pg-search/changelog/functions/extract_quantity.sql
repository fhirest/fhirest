CREATE OR REPLACE FUNCTION search.extract_quantity(_type text, _content jsonb, _path text)
RETURNS TABLE (number numeric, system text, code text, unit text)
AS $$
  WITH paths as (select p.element, (p.path->>'value')::text[] val_path, (p.path->>'system')::text[] sys_path, (p.path->>'code')::text[] code_path, (p.path->>'unit')::text[] unit_path FROM search.subpaths(_type, _path, 'quantity') p),
       data  as (SELECT paths.*, unnest(search.jsonpath(_content, paths.element)) d from paths)
  SELECT (data.d#>>val_path)::numeric, (data.d#>>sys_path)::text, (data.d#>>code_path)::text, (data.d#>>unit_path)::text FROM data WHERE data.d is not null and (d#>>val_path) is not null
$$ LANGUAGE SQL IMMUTABLE;
