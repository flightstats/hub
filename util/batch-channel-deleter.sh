#!/bin/sh
# This will delete all the channels listed in a file
# batch-channel-deleter.sh toDelete.txt
while read line
do
    name=$line
    echo "deleting - $name"
    curl -i -X DELETE ${name}
done < $1
