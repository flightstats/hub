#!/bin/bash
#
# Restores a split and backed up file from the hub index item.
#

INDEXURI=$1

set -e

echo Determining filename...
FILENAME=`curl -s -I $INDEXURI | grep Content-Type | sed -e "s/.*text\/plain.//" | tr -d '\r'`
echo Filename is \"$FILENAME\"
INDEXFILE=$FILENAME.idx

echo Fetching index...
curl -s $INDEXURI > $INDEXFILE

TMPDIR=$FILENAME.tmp

if [ -d $TMPDIR ] ; then
	rm -rf $TMPDIR
fi
mkdir $TMPDIR
while read line; do
	EXT=`echo $line | awk '{print $1}' | tr -d '\r'`
	URI=`echo $line | awk '{print $2}' | tr -d '\r'`
	echo fetching $URI into $EXT...
	curl -s -o $TMPDIR/$FILENAME.$EXT $URI
done < $INDEXFILE
rm $INDEXFILE
cat $TMPDIR/$FILENAME.* > $FILENAME
rm -rf $TMPDIR

echo file complete: $FILENAME
