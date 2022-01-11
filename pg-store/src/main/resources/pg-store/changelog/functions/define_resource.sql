CREATE OR REPLACE FUNCTION fhir.define_resource(_type text) RETURNS void AS 
$$
DECLARE 
_tbl_name text;
BEGIN
  _tbl_name := lower(_type);
  IF EXISTS(SELECT * FROM pg_class WHERE relname = _tbl_name) THEN
    RETURN;
  END IF;
  
  EXECUTE FORMAT('create table %I partition of resource for values in (%L)', _tbl_name, _type);
  EXECUTE FORMAT('alter table %I alter column type set default %L', _tbl_name, _tbl_name);
  EXECUTE FORMAT('select core.create_table_metadata(%L);', _tbl_name);
  EXECUTE FORMAT('CREATE TRIGGER insert_%s_trigger BEFORE INSERT ON %I FOR EACH ROW EXECUTE PROCEDURE resource_insert_trigger();', _tbl_name, _tbl_name);
  
  EXECUTE FORMAT('create index %s_reference_idx on %I using gin (ref(%L,id))', _tbl_name, _tbl_name, _tbl_name);
  EXECUTE FORMAT('create unique index %s_udx on %I (id,last_version)', _tbl_name, _tbl_name);
  EXECUTE FORMAT('alter table %I add primary key (key)', _tbl_name);
  
END;
$$ LANGUAGE plpgsql;