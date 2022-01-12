#!/bin/bash

if [ -z "$DB_NAME" ]; then
   DB_NAME="kefhirdb"
fi

if [ -z "$DB_USER" ]; then
   DB_USER="kefhir"
fi

if [ -z "$DB_PASSWORD" ]; then
   DB_PASSWORD=$DB_USER
fi

psql -d "$POSTGRES_DB" -U "$POSTGRES_USER" -w -f /docker-entrypoint-initdb.d/create_user.psql -v user=$DB_USER -v password="'$DB_PASSWORD'"
psql -d "$POSTGRES_DB" -U "$POSTGRES_USER" -w -f /docker-entrypoint-initdb.d/create_database.psql -v db=$DB_NAME -v user=$DB_USER
psql -d "$DB_NAME" -U "$POSTGRES_USER" -w -f /docker-entrypoint-initdb.d/init_database.psql

