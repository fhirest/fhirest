CREATE OR REPLACE FUNCTION core.session_user() RETURNS jsonb AS
$BODY$
declare 
  l_str text;
begin
  select current_setting('core.client_identifier')::jsonb into l_str; 
  return l_str;
end;
$BODY$
LANGUAGE plpgsql VOLATILE;