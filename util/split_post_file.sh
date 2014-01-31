#!/bin/bash
#
# Split up a file into chunks and post it to a hub channel
#

function usage {
	echo tbd
}

CHANURI=$1
FILE=$2
SIZE=$3

set -e

FILENAME=`basename $FILE`
FILEDIR=`dirname $FILE`
DIR=$FILE.tmp
LISTFILE=$FILE.list

if [ -d $DIR ] ; then
	rm -rf $DIR
fi
echo organizing and splitting tarball
mkdir $DIR
cp $FILE $DIR
cd $DIR && split -b $SIZE $FILENAME $FILENAME.
rm $DIR/$FILENAME

echo starting datahub push
if [ -e $LISTFILE ] ; then
	rm $LISTFILE
fi
for f in `ls -1t $DIR` ; do
	echo posting $DIR/$f
	LOC=`curl -s -i -X POST \
		--data-binary "@$DIR/$f" \
		$CHANURI | grep Location`
	echo $LOC 
	EXT=`echo $f | sed -e "s/.*\.//"`
	LOC=`echo $LOC | awk '{print $2}'`
	echo $EXT $LOC >> $LISTFILE
done

echo all parts posted, uploading index file
INDEX_ITEM=`curl -s -i -X POST \
	--header "Content-type: text/plain+$FILENAME" \
	--data-binary "@$LISTFILE" \
	$CHANURI | grep Location`

echo cleaning up...
rm $LISTFILE
rm -rf $DIR

echo 
echo Backup complete.  Index is here:
echo $INDEX_ITEM
