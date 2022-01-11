#!/bin/bash
fhir="http://localhost:8181"
path=/tmp/kephir
cd /tmp

rm -rf kephir
mkdir kephir && cd kephir
mkdir downloads && cd downloads
wget http://www.hl7.org/fhir/definitions.json.zip &&\
  unzip definitions.json.zip

mv profiles-resources.json profiles-types.json search-parameters.json extension-definitions.json ..
cd .. && rm -rf downloads

curl -XPOST "$fhir/conformance-tools/import-file?file=$path"
#cd .. && rm -rf kephir
