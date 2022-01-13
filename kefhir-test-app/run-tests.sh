#!/bin/bash
cd `dirname $0`
trap ctrlc INT
APP_PORT=48181
DB_PORT=45151
DB_DOCKER_NAME=kefhir-test-postgres
tests=(
  basic-crud.postman_collection.json
)

function main() {
  mkdir -p test-reports
  rm -rf test-reports/*

  makemeadb
  startkefhir
  runtests
  exitcode=$?

  echo 'done'
  [[ $exitcode -eq 0 ]] && echo "tests OK" || echo "tests FAILED"
  finish $exitcode
}

function runtests() {
    cd postman
    for t in "${tests[@]}"; do
      newman run $t \
        --env-var "kefhir=http://localhost:$APP_PORT" \
        --env-var "access_token=yupi"
      r=$?
      [[ $r -ne 0 ]] && break
    done
    cd -
    return $r
}

function ctrlc() {
  finish 1
}

function finish() {
  ex=$1
  [[ -z "$ex" ]] && ex = 0
  echo 'cleaning up...'
  docker rm -vf $DB_DOCKER_NAME >/dev/null 2>&1
  kill -9 `jobs -p` >/dev/null 2>&1
  exit $ex
}

function makemeadb() {
  echo "creating database..."
  docker run -d -e TZ=Europe/Tallinn --restart=unless-stopped --name $DB_DOCKER_NAME -p $DB_PORT:5432 docker.kodality.com/postgres-docker:14 > test-reports/db.log
  sleep 6
  docker exec -e "DB_NAME=kefhirdb" -e "USER_PREFIX=kefhir" $DB_DOCKER_NAME /opt/scripts/createdb.sh >> test-reports/db.log || finish 1
  echo "database created."
}

function startkefhir() {
  echo "starting app..."
  export DB_URL="jdbc:postgresql://localhost:$DB_PORT/kefhirdb"
  [[ -f '/.dockerenv' ]] && export DB_URL="jdbc:postgresql://172.17.0.1:$DB_PORT/kefhirdb"
  export APP_PORT=$APP_PORT
  ../gradlew run 2>&1 >test-reports/server.log &
  PID=$!

  if [ -d /tmp/kefhir ]; then
    while ! grep -m1 'Startup completed' < test-reports/server.log; do sleep 1; done
  fi
  ../etc/download-fhir-definitions.sh "http://localhost:$APP_PORT"

  while true; do
    if ! ps -p $PID > /dev/null; then
      echo 'app failed to start...'
      finish 1
    fi
    curl "http://localhost:$APP_PORT/Patient" -s -o /dev/null -f && break
    sleep 1
  done;
  sleep 5
  echo "app started up."
}

main "$@"
