CREATE OR REPLACE FUNCTION search.create_blindex(
  _param_type    text,
  _resource_type text,
  _path          text,
  _sp_code       text DEFAULT NULL
) RETURNS search.blindex AS $$
DECLARE
  _idx_name  text;
  _base_idx  text;
  _blindex   search.blindex;
  _label     text;
BEGIN
    IF EXISTS (
      SELECT 1 FROM search.blindex
       WHERE resource_type = _resource_type
         AND path          = _path
         AND param_type    = _param_type
    ) THEN
      SELECT * INTO _blindex
        FROM search.blindex
       WHERE resource_type = _resource_type
         AND path          = _path
         AND param_type    = _param_type;
      RETURN _blindex;
    END IF;

    -- ✳️ Minimal normalization for canonical leaves → URI
    IF lower(_param_type) = 'reference'
       AND position('.resourceReference' in _path) = 0
       AND right(_path, 9) = '.resource' THEN
      _param_type := 'uri';
    END IF;

    IF lower(_param_type) = 'string'
       AND position('.resourceReference' in _path) = 0
       AND right(_path, 9) = '.resource' THEN
      _param_type := 'uri';
    END IF;

    -- 🔧 Physical partition table name: no hash (dev)
    _idx_name :=
      lower(_resource_type) || '_' || lower(_param_type) || '_' ||
      lower(replace(regexp_replace(_path, '[^a-zA-Z0-9_]', '_', 'g'), '.', '_'));
    _idx_name := left(_idx_name, 63);

    -- 🏷️ index_name = SP code (sanitized); fallback to physical name if code not provided
    _label := lower(coalesce(_sp_code, ''));
    IF _label = '' THEN _label := _idx_name; END IF;
    _label := left(_label, 63);

    INSERT INTO search.blindex (resource_type, path, param_type, index_name)
    VALUES (_resource_type, _path, _param_type, _label);

    SELECT * INTO _blindex
      FROM search.blindex
     WHERE resource_type = _resource_type
       AND path          = _path
       AND param_type    = _param_type;

    _base_idx := CASE
                   WHEN _param_type = 'string'   THEN 'base_index_string'
                   WHEN _param_type = 'token'    THEN 'base_index_token'
                   WHEN _param_type = 'reference'THEN 'base_index_reference'
                   WHEN _param_type = 'date'     THEN 'base_index_date'
                   WHEN _param_type = 'number'   THEN 'base_index_number'
                   WHEN _param_type = 'quantity' THEN 'base_index_quantity'
                   WHEN _param_type = 'uri'      THEN 'base_index_uri'
                   ELSE NULL
                 END;
    IF _base_idx IS NULL THEN
      RAISE EXCEPTION 'unknown type %', _param_type;
    END IF;

    EXECUTE FORMAT(
      'CREATE TABLE search.%I PARTITION OF search.%I FOR VALUES IN (%L)',
      _idx_name, _base_idx, _blindex.id
    );
    EXECUTE FORMAT(
      'ALTER TABLE search.%I ADD CONSTRAINT fk_%s_%s FOREIGN KEY (sid) REFERENCES search.%I(sid)',
      _idx_name, _idx_name, lower(_resource_type), lower(_resource_type)
    );
    RETURN _blindex;
END;
$$ LANGUAGE plpgsql VOLATILE;
