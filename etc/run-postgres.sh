#!/usr/bin/env bash
cd `dirname $0`


CONTAINER_NAME="kefhir-postgres"

docker rm -vf $CONTAINER_NAME
docker run -d \
 -e TZ=Europe/Tallinn \
 --restart=unless-stopped \
 --name $CONTAINER_NAME \
 -p 5151:5432 \
 docker.kodality.com/postgres-docker:14

sleep 3
docker exec -e "DB_NAME=kefhirdb" -e "USER_PREFIX=kefhir" $CONTAINER_NAME /opt/scripts/createdb.sh
