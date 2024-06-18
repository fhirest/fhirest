

f=`dirname $0`/deps.md


./gradlew fhir-test-app:dependencies \
  | sed -n "s/.*\+\-\- \(.*\)/\1/p" \
  | grep -v "^project" \
  | grep -v "(n)" \
  | sed 's/\(:.*\)\? \-> /:/g' \
  | sed 's/ ([\*c])$//g' \
  | sed  's/$/  /g' \
  | sort \
  | uniq \
  > $f
