CREATE OR REPLACE FUNCTION search.subpaths(_resource_type text, _path text, _param_type text) RETURNS table(path jsonb, element text) AS $$
  with struct as (select * from search.resource_structure_recursive r where parent = _resource_type and (r.alias = _path or r.path = _path))
  select jsonb_array_elements(sc.path), struct.path
    from search.search_configuration sc, struct
    where sc.element_type = struct.element_type
    AND param_type = _param_type;
$$ LANGUAGE SQL IMMUTABLE;
