CREATE OR REPLACE FUNCTION subpaths(_resource_type text, _path text, _param_type text) RETURNS SETOF jsonb AS $$
  select unnest(path) from search_configuration
    where element_type IN (select element_type from resource_structure_recursive where base = _resource_type and path = _path)
    AND param_type = _param_type;
$$ LANGUAGE SQL IMMUTABLE;