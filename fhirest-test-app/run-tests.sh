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
  runtests
  exitcode=$?

  echo 'done'
  [[ $exitcode -eq 0 ]] && echo "tests OK" || echo "tests FAILED"
  finish $exitcode
}

function runtests() {
    cd postman
    for t in ./*; do
      newman run $t \
        --env-var "fhirest=http://localhost:$APP_PORT/fhir" \
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
  kill -15 `jobs -p` >/dev/null 2>&1
  docker rm -vf $DB_DOCKER_NAME >/dev/null 2>&1
  exit $ex
}

function makemeadb() {
  echo "creating database..."
  docker rm -vf $DB_DOCKER_NAME >/dev/null 2>&1
  ../etc/run-postgres.sh $DB_DOCKER_NAME $DB_PORT || exit 1
  echo "database created."
}

function startfhirest() {
  echo "starting app..."
  export DB_URL="jdbc:postgresql://localhost:$DB_PORT/fhirestdb"
  [[ -f '/.dockerenv' ]] && export DB_URL="jdbc:postgresql://172.17.0.1:$DB_PORT/fhirestdb"
  export APP_PORT=$APP_PORT
  >test-reports/server.log
  ../gradlew run 2>&1 | tee test-reports/server.log &
  PID=$!

#  while ! grep -m1 'Startup completed' < test-reports/server.log; do sleep 1; done
#  ../etc/download-fhir-definitions.sh "http://localhost:$APP_PORT" || finish 1

  while true; do
    if ! ps -p $PID >/dev/null 2>&1; then exit 1; fi
    if ! grep -m1 'conformance loaded' < test-reports/server.log; then continue; fi
    if ! grep -m1 'blindex initialization finished' < test-reports/server.log; then continue; fi
    break;
  done
  sleep 5
  echo "app started up."
}

main "$@"






#  while true; do
#    if ! ps -p $PID > /dev/null; then
#      echo 'app failed to start...'
#      finish 1
#    fi
#    curl "http://localhost:$APP_PORT/Patient" -s -o /dev/null -f && break
#    sleep 1
#  done;
