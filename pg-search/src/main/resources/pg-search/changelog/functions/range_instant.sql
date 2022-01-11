CREATE OR REPLACE FUNCTION range_instant(_ts timestamptz) RETURNS tstzrange AS $$
BEGIN
  return tstzrange('[' || _ts || ',' || _ts || ']');
END;
$$ LANGUAGE plpgsql IMMUTABLE;