CREATE OR REPLACE FUNCTION search.range(_date text, _interval text) RETURNS tstzrange AS $$
DECLARE
  _ts timestamptz;
BEGIN
  --_ts := to_timestamp(_date, 'YYYY-MM-DD"T"HH24:MI:SS');
  --return tstzrange('[' || _ts || ',' || (_ts + _interval::interval) || ')');
  return tstzrange('[' || _date::timestamptz || ',' || (_date::timestamptz + _interval::interval) || ')');
END;
$$ LANGUAGE plpgsql IMMUTABLE;
