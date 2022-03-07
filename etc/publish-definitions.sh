#!/bin/bash
repo="http://www.hl7.org/fhir/definitions.json.zip"
u=$KODALITY_NEXUS_USER
p=$KODALITY_NEXUS_PASSWORD
if [ -f ~/.gradle/gradle.properties ]; then
  [[ -z $u ]] && u=`cat ~/.gradle/gradle.properties | sed -n 's/kodalityNexusUser=\(.*\)/\1/p'`
  [[ -z $p ]] && p=`cat ~/.gradle/gradle.properties | sed -n 's/kodalityNexusPassword=\(.*\)/\1/p'`
fi
[[ -z $u ]] && echo "no username" && exit 1

mkdir -p /tmp/kefhir/downloads && cd /tmp/kefhir/downloads
wget $repo
for t in ./*; do
  curl -v -u $u:$p --upload-file $t https://kexus.kodality.com/repository/store-public/kefhir/$t
done
cd .. && rm -rf downloads
