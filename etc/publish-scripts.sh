#!/bin/bash
cd `dirname $0`
u=$KODALITY_NEXUS_USER
p=$KODALITY_NEXUS_PASSWORD
if [ -f ~/.gradle/gradle.properties ]; then
  [[ -z $u ]] && u=`cat ~/.gradle/gradle.properties | sed -n 's/kodalityNexusUser=\(.*\)/\1/p'`
  [[ -z $p ]] && p=`cat ~/.gradle/gradle.properties | sed -n 's/kodalityNexusPassword=\(.*\)/\1/p'`
fi
[[ -z $u ]] && echo "no username" && exit 1
curl -v -u $u:$p --upload-file download-fhir-definitions.sh https://kexus.kodality.com/repository/store-public/kefhir/download-fhir-definitions.sh
