CREATE OR REPLACE FUNCTION search.jsonpath(json jsonb[], path text[]) RETURNS jsonb[] AS $$
DECLARE 
  result jsonb[];
  element jsonb;
BEGIN 
  result := array[]::jsonb[];
  FOREACH element IN ARRAY json LOOP
    result := array_cat(result, search.jsonpath(element, path));
  END LOOP;
  return result;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
