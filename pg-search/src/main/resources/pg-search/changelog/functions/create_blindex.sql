CREATE OR REPLACE FUNCTION search.create_blindex(_resource_type text, _path text) RETURNS search.blindex AS $$
DECLARE
  _struct search.resource_structure;
  _param_type text;
  _idx_name text;
  _base_idx text;
  _blindex search.blindex;
BEGIN
  IF NOT EXISTS(SELECT 1 FROM search.resource_structure_recursive WHERE base = _resource_type and path = _path) THEN
    RAISE EXCEPTION '% not found in resource_structure', _resource_type || '.' || _path;
  END IF;
  FOR _struct IN (SELECT * FROM search.resource_structure_recursive WHERE base = _resource_type  and path = _path) LOOP
    IF NOT EXISTS (SELECT 1 FROM search.search_configuration WHERE element_type = _struct.element_type) THEN
      RAISE EXCEPTION '% not configured. (search_configuration)', _struct.element_type;
    END IF;
    SELECT param_type into _param_type FROM search.search_configuration WHERE element_type = _struct.element_type;
    IF EXISTS (SELECT 1 FROM search.blindex WHERE resource_type = _resource_type AND path = _path) THEN
      SELECT * into _blindex FROM search.blindex WHERE resource_type = _resource_type AND path = _path;
      return _blindex;
    END IF;

    _idx_name := lower(_resource_type) || '_' || _param_type || '_' || lower(replace(_path, '.', '_'));

    INSERT INTO search.blindex (resource_type, path, param_type, index_name) values (_resource_type, _path, _param_type, _idx_name);
    SELECT * into _blindex FROM search.blindex WHERE resource_type = _resource_type AND path = _path;

    _base_idx := case
                   when _param_type = 'string' then 'base_index_string'
                   when _param_type = 'token' then 'base_index_token'
                   when _param_type = 'reference' then 'base_index_reference'
                   when _param_type = 'date' then 'base_index_date'
                   when _param_type = 'number' then 'base_index_number'
                   when _param_type = 'quantity' then 'base_index_quantity'
                   else null
                 end;
  if _base_idx is null THEN
    RAISE EXCEPTION 'unknown type %', _param_type;
  end if;

  EXECUTE FORMAT('create table search.%I partition of search.%I for values in (%L)', _idx_name, _base_idx, _blindex.id);
  return _blindex;
  END LOOP;
END;
$$ LANGUAGE plpgsql VOLATILE;
