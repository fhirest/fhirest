CREATE OR REPLACE FUNCTION search.cleanup_indexes() RETURNS void AS $$
DECLARE
  _blindex search.blindex;
BEGIN
  FOR _blindex IN (SELECT * FROM search.blindex) LOOP
    execute format('delete from search.%I where active = false', _blindex.index_name);
  END LOOP;
  delete from search.resource where active = false;
END;
$$ LANGUAGE plpgsql VOLATILE;
