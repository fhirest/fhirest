CREATE OR REPLACE FUNCTION search.drop_blindex(_param_type text, _resource_type text, _path text) RETURNS void AS $$
DECLARE
  _blindex search.blindex;
BEGIN
  FOR _blindex IN (SELECT * FROM search.blindex WHERE resource_type = _resource_type AND path = _path and param_type = _param_type) LOOP
    EXECUTE format('DROP TABLE IF EXISTS search.%1$s', _blindex.index_name);
  END LOOP;
  DELETE FROM search.blindex WHERE resource_type = _resource_type AND path = _path and param_type = _param_type;
END;
$$ LANGUAGE plpgsql VOLATILE;
