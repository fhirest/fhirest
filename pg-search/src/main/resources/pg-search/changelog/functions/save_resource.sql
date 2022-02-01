CREATE OR REPLACE FUNCTION search.save_resource(_id text, _type text, _last_updated timestamptz, _content jsonb) RETURNS void AS $$
DECLARE
  _sid bigint;
  _type_id bigint;
  _blindex_id bigint;
BEGIN
  update search.resource r set last_updated = _last_updated, active = true where resource_type = search.resource_type_id(_type) and resource_id = _id returning sid into _sid;
  if _sid is null then
    insert into search.resource(resource_type, resource_id, last_updated, active)
      values (search.resource_type_id(_type), _id, _last_updated, true)
      returning sid into _sid;
  end if;

  FOR _blindex_id IN (SELECT id FROM search.blindex WHERE resource_type = _type) LOOP
    perform search.merge_blindex(_blindex_id, _sid, _content);
  END LOOP;
END;
$$ LANGUAGE plpgsql VOLATILE;
