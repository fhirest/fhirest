#!/bin/bash

# Usage:
# set environment variables: SONATYPE_USER, SONATYPE_PASSWORD
# ./publish-to-maven-central.sh VERSION [MODULE]
# eg: ./publish-to-maven-central.sh 1.0.0

modules=(
fhir-structures
fhirest-core
tx-manager
fhir-conformance
fhir-rest
fhirest-scheduler
validation-profile
pg-core
pg-store
pg-search
auth-core
auth-openid
auth-smart
auth-rest
feature-conditional-reference
operation-patient-everything
fhirest-blockchain openapi
)

ver=$1
module=$2
[[ -z "$ver" ]] && echo "give me a version" && exit 1
gr=./gradlew
out=publish
user=$SONATYPE_USER
pw=$SONATYPE_PASSWORD
auth=$(echo "${user}:${pw}" | base64)

build() {
  gradle -Pversion=$ver clean assemble generatePom sign || exit 1
}

publish() {
  p=$1
  mkdir $out/$p

  cp -r $p/build/libs/* $out/$p
  cp -r $p/build/publications/mavenJava/pom-default.xml $out/$p/$p-$ver.pom
  cp -r $p/build/publications/mavenJava/pom-default.xml.asc $out/$p/$p-$ver.pom.asc

  cd $out/$p
  for file in ./*; do md5sum "$file" | awk '{print $1}' > "$file".md5; done
  for file in ./*; do sha1sum  "$file" | awk '{print $1}' > "$file".sha1; done
  rm -rf *.md5.sha1

  mkdir -p ee/fhir/fhirest/$p/$ver
  mv * ee/fhir/fhirest/$p/$ver
  zip=$p-$ver.zip
  zip -r $zip ee

  curl -XPOST -H "Authorization: Bearer $auth" --form bundle=@$zip -D - "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC"
  cd -
}


rm -rf $out && mkdir -p $out || exit 1

if [ ! -z "$module" ]; then
  cd $module || (echo "no such module" && exit 1)
  build
  cd -

  publish $module
  exit 0
fi

build
for p in ${modules[@]}; do
  publish $p
done



