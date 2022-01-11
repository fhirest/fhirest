CREATE OR REPLACE FUNCTION core.sys_columns()
  RETURNS trigger AS
$BODY$
DECLARE
  l_client_id   VARCHAR := null;
  l_ts          TIMESTAMPTZ := NOW();
  sys_column    text;
BEGIN
  FOR sys_column IN 
    SELECT column_name::text
       FROM information_schema.columns
       WHERE table_name = TG_TABLE_NAME
       AND column_name like 'sys%'
  LOOP
    
    IF sys_column = 'sys_status' THEN
      IF new.sys_status IS NULL THEN
        new.sys_status = CASE WHEN TG_OP = 'INSERT' THEN 'A'
                              WHEN TG_OP = 'UPDATE' THEN old.sys_status END; 
      END IF;
      
    ELSEIF sys_column = 'sys_version' THEN
        new.sys_version = CASE WHEN TG_OP = 'INSERT' THEN coalesce(new.sys_version,0)+1
                               WHEN TG_OP = 'UPDATE' THEN coalesce(new.sys_version,old.sys_version,0)+1 END; 
                               
    ELSEIF sys_column = 'sys_modified' THEN
      new.sys_modified = core.footprint();
      
    ELSEIF sys_column = 'sys_created' THEN
      IF TG_OP = 'INSERT'
        THEN new.sys_created = core.footprint();
      END IF;
    
    END IF;
  END LOOP;
  
  RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 10;


