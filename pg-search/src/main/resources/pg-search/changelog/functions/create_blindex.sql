CREATE OR REPLACE FUNCTION search.create_blindex(_param_type text, _resource_type text, _path text) RETURNS search.blindex AS $$
DECLARE
  _idx_name text;
  _base_idx text;
  _blindex search.blindex;
BEGIN
    IF EXISTS (SELECT 1 FROM search.blindex WHERE resource_type = _resource_type AND path = _path and param_type = _param_type) THEN
      SELECT * into _blindex FROM search.blindex WHERE resource_type = _resource_type AND path = _path and param_type = _param_type;
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
                   when _param_type = 'uri' then 'base_index_uri'
                   else null
                 end;
  if _base_idx is null THEN
    RAISE EXCEPTION 'unknown type %', _param_type;
  end if;

  EXECUTE FORMAT('create table search.%I partition of search.%I for values in (%L)', _idx_name, _base_idx, _blindex.id);
  EXECUTE FORMAT('alter table search.%I add constraint fk_%s_%s foreign key (sid) references search.%I(sid)', _idx_name, _idx_name, lower(_resource_type), lower(_resource_type));
  return _blindex;
END;
$$ LANGUAGE plpgsql VOLATILE;
