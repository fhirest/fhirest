CREATE OR REPLACE FUNCTION search.system_id(_system text) RETURNS bigint AS $$
    with t as (select id from search.system where system.system = coalesce(_system, '')),
         i as (insert into search.system(system) select coalesce(_system, '') where not exists (select 1 from t) returning id)
    select coalesce((select id from t), (select id from i))
$$ LANGUAGE SQL volatile;
