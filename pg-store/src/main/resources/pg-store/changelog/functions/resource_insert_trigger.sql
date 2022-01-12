CREATE OR REPLACE FUNCTION store.resource_insert_trigger() RETURNS TRIGGER AS
$$
BEGIN
  new.id := coalesce(new.id, new.key::text);
  UPDATE store.resource SET sys_status = 'T' WHERE id = new.id AND type = new.type;
  return new;
END;
$$ LANGUAGE plpgsql VOLATILE;
