CREATE OR REPLACE FUNCTION search.merge_blindex(_blindex_id bigint, _sid bigint, _content jsonb) RETURNS void AS $$
DECLARE
  _blindex search.blindex;
BEGIN
  select * into _blindex from search.blindex where id = _blindex_id;
  if _content is null then
    EXECUTE format('UPDATE search.%I set active = false WHERE sid = %L', _blindex.index_name, _sid);
    return;
  end if;

  CASE
    WHEN _blindex.param_type = 'string' THEN
      EXECUTE
        format('with values as (select string from search.extract_string(%L, %L, %L)),', _blindex.resource_type, _content, _blindex.path) ||
        format('deleted as (update search.%I set active = false where sid = %L and active = true and string not in (select * from values)),', _blindex.index_name, _sid) ||
        format('created as (insert into search.%I(sid, blindex_id, string) select %L, %L, string from values
          where string not in (select string from search.%I where active = true and sid = %L))', _blindex.index_name, _sid, _blindex_id, _blindex.index_name, _sid) ||
        'select 1';
    WHEN _blindex.param_type = 'token' THEN
      EXECUTE
        format('with values as (select search.system_id(system) system_id, value from search.extract_token(%L, %L, %L)),', _blindex.resource_type, _content, _blindex.path) ||
        format('deleted as (update search.%I set active = false where sid = %L and active = true and (system_id, value) not in (select * from values)),', _blindex.index_name, _sid) ||
        format('created as (insert into search.%I(sid, blindex_id, system_id, value) select %L, %L, system_id, value from values
          where (system_id, value) not in (select system_id, value from search.%I where active = true and sid = %L))', _blindex.index_name, _sid, _blindex_id, _blindex.index_name, _sid) ||
        'select 1';
    WHEN _blindex.param_type = 'reference' THEN
      EXECUTE
        format('with values as (select search.resource_type_id(type) type_id, id from search.extract_reference(%L, %L, %L)),', _blindex.resource_type, _content, _blindex.path) ||
        format('deleted as (update search.%I set active = false where sid = %L and active = true and (type_id, id) not in (select * from values)),', _blindex.index_name, _sid) ||
        format('created as (insert into search.%I(sid, blindex_id, type_id, id) select %L, %L, type_id, id from values
          where (type_id, id) not in (select type_id, id from search.%I where active = true and sid = %L))', _blindex.index_name, _sid, _blindex_id, _blindex.index_name, _sid) ||
        'select 1';
    WHEN _blindex.param_type = 'date' THEN
      EXECUTE
        format('with values as (select range from search.extract_date(%L, %L, %L)),', _blindex.resource_type, _content, _blindex.path) ||
        format('deleted as (update search.%I set active = false where sid = %L and active = true and range not in (select * from values)),', _blindex.index_name, _sid) ||
        format('created as (insert into search.%I(sid, blindex_id, range) select %L, %L, range from values
          where range not in (select range from search.%I where active = true and sid = %L))', _blindex.index_name, _sid, _blindex_id, _blindex.index_name, _sid) ||
        'select 1';
    ELSE
      RAISE EXCEPTION 'unknown type %', _blindex.param_type;
  END CASE;
END;
$$ LANGUAGE plpgsql VOLATILE;

