#!/bin/bash
fhir="$1"
[[ -z "$fhir" ]] && fhir="http://localhost:8181"
path=/tmp/kefhir

if [ ! -d $path ]; then
  mkdir -p $path && cd $path
  mkdir downloads && cd downloads
  #wget http://www.hl7.org/fhir/definitions.json.zip &&\
  wget https://kexus.kodality.com/repository/store-public/fhir/definitions.json.zip &&\
    unzip definitions.json.zip
  rm -rv definitions.json.zip fhir.schema.json.zip version.info
  mv * ..
  cd .. && rm -rf downloads
fi

curl -XPOST "$fhir/conformance-tools/import-file?file=$path"
