CREATE OR REPLACE FUNCTION ref(type text, id text) RETURNS text[] AS 
$$
    SELECT ARRAY[type || '/' || id]::text[]
$$ LANGUAGE SQL;
