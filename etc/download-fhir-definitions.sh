#!/bin/bash
fhir="$1"
[[ -z "$fhir" ]] && fhir="http://localhost:8181"
temp=/tmp/fhirest
repo="https://kexus.kodality.com/repository/store-public/kefhir/defs-r5.zip"
#repo="http://hl7.org/fhir/5.0.0-draft-final/definitions.json.zip"
#repo="https://kexus.kodality.com/repository/store-public/kefhir/definitions.json.zip"

if [ ! -d $temp ]; then
  mkdir -p $temp && cd $temp
  mkdir downloads && cd downloads
  wget $repo -O definitions.json.zip && unzip definitions.json.zip
  rm -rv definitions.json.zip fhir.schema.json.zip version.info
  mv * ..
  cd .. && rm -rf downloads
fi

curl -XPOST "$fhir/conformance-tools/import-file?file=$temp"
