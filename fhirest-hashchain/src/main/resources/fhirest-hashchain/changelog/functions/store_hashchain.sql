CREATE OR REPLACE FUNCTION store.store_hashchain(_uid bigint) RETURNS void AS $$
BEGIN
  with t as (select
    (select id from store.hashchain_config where sys_status = 'A') as config_id,
    (select id from store.hashchain order by id desc limit 1) as previous_id
  )
  insert into store.hashchain(config_id, previous_hash_id, resource_uid, hash)
  select config_id, previous_id, _uid, store.calculate_hashchain(_uid, config_id, previous_id) from t;
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION store.store_hashchain(_type text, _resource_id text, _version smallint) RETURNS void AS $$
BEGIN
perform store.store_hashchain(r.uid)
from store.resource r where r.type = _type and r.id = _resource_id and r.version = _version;
END
$$ LANGUAGE plpgsql;

