CREATE OR REPLACE FUNCTION search.jsonpath(json jsonb, path text[]) RETURNS jsonb[] AS $$
BEGIN 
  IF jsonb_typeof(json) = 'array' THEN
     return search.jsonpath(ARRAY(select jsonb_array_elements(json)), path);
  END IF;
  IF array_length(path, 1) IS NULL THEN
    return array[json#>'{}'];
  END IF;
  IF jsonb_typeof(json) <> 'object' THEN
     return array[]::jsonb[];
  END IF;
  RETURN search.jsonpath(json->path[1], path[2:array_length(path, 1)]);
END;
$$ LANGUAGE plpgsql IMMUTABLE;
