#!/usr/bin/env bash
CONTAINER_NAME="$1"
[[ -z "$CONTAINER_NAME" ]] && CONTAINER_NAME="kefhir-postgres"
PORT="$2"
[[ -z "$PORT" ]] && PORT="5151"

docker rm -vf $CONTAINER_NAME
docker run -d \
 -e TZ=Europe/Tallinn \
 --restart=unless-stopped \
 --name $CONTAINER_NAME \
 -e POSTGRES_PASSWORD=postgres \
 -p $PORT:5432 \
 postgres:14

sleep 3
docker exec -i $CONTAINER_NAME psql -U postgres <<-EOSQL
CREATE ROLE kefhir_admin LOGIN PASSWORD 'test' NOSUPERUSER INHERIT NOCREATEDB CREATEROLE NOREPLICATION;
CREATE ROLE kefhir_app   LOGIN PASSWORD 'test' NOSUPERUSER INHERIT NOCREATEDB CREATEROLE NOREPLICATION;
CREATE DATABASE kefhirdb WITH OWNER = kefhir_admin ENCODING = 'UTF8' TABLESPACE = pg_default CONNECTION LIMIT = -1;
grant temp on database kefhirdb to kefhir_app;
grant connect on database kefhirdb to kefhir_app;
EOSQL
