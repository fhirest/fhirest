#!/bin/bash
cd `dirname $0`

fhirest=$1
[[ -z "$fhirest" ]] && fhirest="http://localhost:8181/fhir"

cd postman
for t in ./*; do
  newman run $t \
    --env-var "fhirest=$fhirest" \
    --env-var "access_token=yupi"
  r=$?
  [[ $r -ne 0 ]] && break
done
cd -
exit $r

