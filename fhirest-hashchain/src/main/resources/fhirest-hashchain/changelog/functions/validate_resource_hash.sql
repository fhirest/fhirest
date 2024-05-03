CREATE OR REPLACE FUNCTION store.validate_resource_hash(_uid bigint) RETURNS table(calculated text, stored text) AS $$
select store.calculate_hashchain(resource_uid, config_id, previous_hash_id), hash
from store.hashchain where resource_uid = _uid;
$$ LANGUAGE SQL volatile;


CREATE OR REPLACE FUNCTION store.validate_resource_hash(_type text, _resource_id text, _version smallint) RETURNS table(calculated text, stored text) AS $$
BEGIN
select store.validate_resource_hash(r.uid)
from store.resource r where r.type = _type and r.id = _resource_id and r.version = _version;
END
$$ LANGUAGE plpgsql;
