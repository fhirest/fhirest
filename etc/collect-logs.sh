
search=`pwd`
cd `dirname $0`
f=logs.md
echo "| file | log |" > $f
echo "| ---- | --- |" >> $f
grep -r --include=*.java "log\." $search | sed 's/ \{2,\}/ /g' | sed 's/\(.*\.java\): \(.*\)/|\1|\2|/g' >> $f
