CREATE OR REPLACE FUNCTION search.delete_resource(_id text, _type text) RETURNS void AS $$
DECLARE
  _sid bigint;
  _type_id bigint;
  _blindex_id bigint;
BEGIN
  update search.resource r set active = false where resource_type = (select search.resource_type_id(_type)) and resource_id = _id returning sid into _sid;
  if _sid is null then
    return;
  end if;

  FOR _blindex_id IN (SELECT id FROM search.blindex WHERE resource_type = _type) LOOP
    perform search.merge_blindex(_blindex_id, _sid, null);
  END LOOP;
END;
$$ LANGUAGE plpgsql VOLATILE;
