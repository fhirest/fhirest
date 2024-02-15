#!/bin/bash
cd `dirname $0`
trap ctrlc INT
APP_PORT=48181
DB_PORT=45151
DB_DOCKER_NAME=fhirest-test-postgres

function main() {
  mkdir -p test-reports
  rm -rf test-reports/*

  makemeadb
  startfhirest
  ./run-newman.sh "http://localhost:$APP_PORT/fhir"
  exitcode=$?

  echo 'done'
  [[ $exitcode -eq 0 ]] && echo "tests OK" || echo "tests FAILED"
  finish $exitcode
}

function ctrlc() {
  finish 1
}

function finish() {
  ex=$1
  [[ -z "$ex" ]] && ex = 0
  echo 'cleaning up...'
  kill -15 `jobs -p` >/dev/null 2>&1
  docker rm -vf $DB_DOCKER_NAME >/dev/null 2>&1
  exit $ex
}

function makemeadb() {
  echo "creating database..."
  docker rm -vf $DB_DOCKER_NAME >/dev/null 2>&1
  ../etc/run-poostgres.sh $DB_DOCKER_NAME $DB_PORT || exit 1
  echo "database created."
}

function startfhirest() {
  echo "starting app..."
  export DB_URL="jdbc:postgresql://localhost:$DB_PORT/fhirestdb"
  [[ -f '/.dockerenv' ]] && export DB_URL="jdbc:postgresql://172.17.0.1:$DB_PORT/fhirestdb"
  export SERVER_PORT=$APP_PORT
  >test-reports/server.log
  ../gradlew bootRun 2>&1 | tee test-reports/server.log &
  PID=$!

  while true; do
    sleep 1
    if ! ps -p $PID >/dev/null 2>&1; then exit 1; fi
    if ! grep -m1 'conformance loaded' < test-reports/server.log; then continue; fi
    if ! grep -m1 'blindex initialization finished' < test-reports/server.log; then continue; fi
    break;
  done
  sleep 5
  echo "app started up."
}

main "$@"

