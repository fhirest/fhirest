CREATE OR REPLACE FUNCTION search.extract_quantity(_type text, _content jsonb, _path text)
RETURNS TABLE (number numeric, system text, code text, unit text)
AS $$
  WITH data as (SELECT unnest(search.jsonpath(_content, _path)) d),
       paths as (select (p->>'value')::text[] val_path, (p->>'system')::text[] sys_path, (p->>'code')::text[] code_path, (p->>'unit')::text[] unit_path FROM search.subpaths(_type, _path, 'quantity') p)
  SELECT (d#>>val_path)::numeric, (d#>>sys_path)::text, (d#>>code_path)::text, (d#>>unit_path)::text FROM data, paths WHERE (d#>>val_path) is not null
$$ LANGUAGE SQL IMMUTABLE;
