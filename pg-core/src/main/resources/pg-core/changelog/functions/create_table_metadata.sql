create or replace function core.create_table_metadata(p_schema varchar, p_table_name varchar) returns varchar language plpgsql volatile as
$BODY$
declare
  rc bigint;
  l_schematable text;
  l_table text;
  sql text;
begin
  l_table := lower(p_table_name);
  l_schematable := lower(p_schema) || '.' || l_table;

  -- create trigger corresponding table trigger type
  sql := 'DROP TRIGGER IF EXISTS thi_' || l_table ||' ON ' || l_schematable || '';
  perform core.exec(sql);
  sql := 'CREATE TRIGGER thi_' || l_table ||' BEFORE INSERT OR UPDATE ON ' || l_schematable ||' FOR EACH ROW EXECUTE PROCEDURE core.sys_columns()';
  perform core.exec(sql);
  
  return '(Y)';
end;
$BODY$;
