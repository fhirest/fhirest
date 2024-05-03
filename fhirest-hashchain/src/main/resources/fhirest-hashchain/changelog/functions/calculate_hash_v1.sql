CREATE OR REPLACE FUNCTION store.calculate_hash_v1(_uid bigint, _alg text) RETURNS text AS $$
  with r as (
    select
      coalesce(r.uid::text, '') || '#' ||
      coalesce(r.type, '')      || '#' ||
      coalesce(r.id, '')        || '#' ||
      coalesce(r.version::text, '') || '#' ||
      coalesce(r.updated::text, '') || '#' ||
      coalesce(r.author::text, '')  || '#' ||
      coalesce(r.content::text, '') as row
    from store.resource r where r.uid = _uid
  )
  select encode(digest(row, _alg), 'hex') hash from r
$$ LANGUAGE SQL immutable;
