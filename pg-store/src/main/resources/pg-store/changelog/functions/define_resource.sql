CREATE OR REPLACE FUNCTION store.define_resource(_type text) RETURNS void AS
$$
DECLARE 
_tbl_name text;
BEGIN
  _tbl_name := lower(_type);
  IF EXISTS(select 1 FROM pg_tables where schemaname = 'store' AND tablename = _tbl_name) THEN
    RETURN;
  END IF;
  
  EXECUTE FORMAT('create table store.%I partition of store.resource for values in (%L)', _tbl_name, _type);
  EXECUTE FORMAT('alter table store.%I alter column type set default %L', _tbl_name, _tbl_name);
  EXECUTE FORMAT('select core.create_table_metadata(''store'', %L);', _tbl_name);
  EXECUTE FORMAT('CREATE TRIGGER insert_%s_trigger BEFORE INSERT ON store.%I FOR EACH ROW EXECUTE PROCEDURE store.resource_insert_trigger();', _tbl_name, _tbl_name);
  
  EXECUTE FORMAT('create index %s_reference_idx on store.%I (type, id) where sys_status = ''A''', _tbl_name, _tbl_name);
  EXECUTE FORMAT('create unique index %s_udx on store.%I (id,version)', _tbl_name, _tbl_name);
  EXECUTE FORMAT('alter table store.%I add primary key (uid)', _tbl_name);
  
END;
$$ LANGUAGE plpgsql;
