#!/usr/bin/env bash
cd `dirname $0`


CONTAINER_NAME="kephir-postgres"
DB_NAME="kephirdb"

docker rm -vf $CONTAINER_NAME
docker run -d \
 -e TZ=Europe/Tallinn \
 -e DB_NAME=$DB_NAME \
 -e POSTGRES_PASSWORD=postgres \
 --restart=unless-stopped \
 --name $CONTAINER_NAME \
 -p 5151:5432 \
 postgres:14.1

docker cp `pwd`/docker-entrypoint-initdb.d $CONTAINER_NAME:/
#docker exec -ti $CONTAINER_NAME /init/createdb.sh
#/usr/local/bin/docker-entrypoint.sh: ignoring /docker-entrypoint-initdb.d/*
