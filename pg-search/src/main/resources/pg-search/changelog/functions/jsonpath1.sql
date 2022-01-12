CREATE OR REPLACE FUNCTION search.jsonpath(json jsonb, path text) RETURNS jsonb[] AS $$
BEGIN 
  RETURN search.jsonpath(json, string_to_array(path, '.'));
END;
$$ LANGUAGE plpgsql IMMUTABLE;
