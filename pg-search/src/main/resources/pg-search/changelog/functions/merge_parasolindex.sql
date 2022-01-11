CREATE OR REPLACE FUNCTION merge_parasolindex() RETURNS TRIGGER AS $$
DECLARE
  _resource resource;
  _blindex blindex;
BEGIN
  _resource := new;
  FOR _blindex IN (SELECT * FROM blindex WHERE index_type = 'parasol' AND resource_type = _resource.type) LOOP
    CASE
      WHEN _blindex.param_type = 'date' THEN
        --EXECUTE format('DELETE FROM %I WHERE resource_key = %s', _blindex.index_name, _resource.key);
        EXECUTE format('INSERT INTO %I (SELECT %s, unnest(''%s''::tstzrange[]))', _blindex.index_name, _resource.key, COALESCE(date(_resource, _blindex.path), '{}'));
      ELSE
        RAISE EXCEPTION 'unknown type %', _blindex.param_type;
    END CASE;
  END LOOP;
  RETURN new;
END;
$$ LANGUAGE plpgsql VOLATILE;

