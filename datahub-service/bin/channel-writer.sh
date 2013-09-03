#!/bin/bash
#
# This script writes data to a channel resource repeatedly
# (as fast as it can with a single thread).
#
# It requires cowsay and fortune.  Yup.
# 

COWFILE=/tmp/cow.txt

function usage {
	echo "Usage: $0 <channel_resource_url>"
}

if [ "$#" != "1" ] ; then
	usage
	exit 1
fi

URL=$1

while [ "1" == "1" ] ; do
	cowsay `fortune` > ${COWFILE}
	echo -n .
	curl -s -X POST --data-binary @${COWFILE} \
		--header "Content-Type: text/plain" \
		${URL} > /dev/null
done
