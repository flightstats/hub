#!/bin/bash
# Clever hack to traverse next/prev links to the end/start of a channel


function usage {
	echo "Usage: $0 <next|previous> <url>"
}

if [ "$#" != "2" ] ; then
	usage
	exit 1
fi

DIR=$1
URL=$2

if [ "${DIR}" != "previous" ] && [ "${DIR}" != "next" ]; then
	usage
	exit 1
fi 


while [ "1" == "1" ] ; do
	echo ${URL}
	URL=`curl -s -I ${URL} | grep ${DIR} | \
		sed -e 's/^Link: <//' | \
		sed -e "s/>;rel=\"${DIR}\".*//"`
	if [ "${URL}" == "" ]; then
		echo 'Finished (no more)'
		break
	fi
done
