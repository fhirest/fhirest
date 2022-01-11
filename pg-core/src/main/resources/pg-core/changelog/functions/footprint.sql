CREATE OR REPLACE FUNCTION core.footprint() RETURNS jsonb AS
$BODY$
begin
  return jsonb_build_object(
    'date', to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SSOF'),
    'user', core.session_user()
  );
end;
$BODY$
LANGUAGE plpgsql VOLATILE;