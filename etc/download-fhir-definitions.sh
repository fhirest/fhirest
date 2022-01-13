#!/bin/bash
fhir="$1"
[[ -z "$fhir" ]] && fhir="http://localhost:8181"
path=/tmp/kefhir

if [ ! -d $path ]; then
  mkdir -p $path && cd $path
  mkdir downloads && cd downloads
  wget http://www.hl7.org/fhir/definitions.json.zip &&\
    unzip definitions.json.zip
  mv profiles-resources.json profiles-types.json search-parameters.json extension-definitions.json ..
  cd .. && rm -rf downloads
fi

curl -XPOST "$fhir/conformance-tools/import-file?file=$path"
