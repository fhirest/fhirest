CREATE OR REPLACE FUNCTION search.rt_id(_type text) RETURNS bigint AS $$
    select id from search.resource_type rt where rt.type = _type
$$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION search.resource_type_id(_type text) RETURNS bigint AS $$
    with t as (select id from search.resource_type rt where rt.type = _type),
         i as (insert into search.resource_type (type) select _type where _type is not null and not exists (select 1 from t) returning id)
    select coalesce((select id from t), (select id from i))
$$ LANGUAGE SQL volatile;
