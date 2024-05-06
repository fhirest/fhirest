CREATE OR REPLACE FUNCTION store.calculate_hashchain(_uid bigint, _config_id bigint, _previous_hash_id bigint) RETURNS text AS $$
with
  config as (
    select * from store.hashchain_config where id = _config_id
  ),
  resource as (
    select (case
	  when version = 'v1' then store.calculate_hash_v1(_uid, config.algorithm)
	  else null end
	) hash from config
  )
select encode(digest(
  coalesce((select hash from store.hashchain where id = _previous_hash_id), resource.hash) || '#' || resource.hash,
  config.algorithm
), 'hex')
from config, resource
$$ LANGUAGE SQL volatile;
