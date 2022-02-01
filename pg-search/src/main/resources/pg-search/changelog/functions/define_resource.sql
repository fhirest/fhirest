CREATE OR REPLACE FUNCTION search.define_resource(_type text) RETURNS void AS
$$
DECLARE 
_tbl_name text;
BEGIN
  _tbl_name := lower(_type);
  IF EXISTS(select 1 FROM pg_tables where schemaname = 'search' AND tablename = _tbl_name) THEN
    RETURN;
  END IF;
  
  EXECUTE FORMAT('create table search.%I partition of search.resource for values in (%L)', _tbl_name, (select search.resource_type_id(_type)));
  EXECUTE FORMAT('create unique index on search.%I (resource_type, resource_id) where active = true', _tbl_name);
  EXECUTE FORMAT('create index on search.%I (last_updated) where active = true', _tbl_name);
END;
$$ LANGUAGE plpgsql;
