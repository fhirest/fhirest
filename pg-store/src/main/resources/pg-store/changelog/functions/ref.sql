CREATE OR REPLACE FUNCTION store.ref(type text, id text) RETURNS text[] AS
$$
    SELECT ARRAY[type || '/' || id]::text[]
$$ LANGUAGE SQL;
