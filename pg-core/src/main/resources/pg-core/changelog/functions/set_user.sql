CREATE OR REPLACE FUNCTION core.set_user(in_user jsonb) RETURNS text AS
$BODY$
declare 
  l_str text;
begin
  SELECT set_config('core.client_identifier', in_user::text, false) into l_str; 
  return l_str;
end;
$BODY$
LANGUAGE plpgsql VOLATILE;