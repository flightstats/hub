#!/bin/bash

#
# This fires up some red speedo instances on a channel directly against
# multiple datahub hosts.  When they all finish, we traverse the channel fetching all
# of the URIs in order.  We then sort the result lexically and compare that result to
# the unsorted version.  If they match, we have succeeded.  If not, we have failed.
#

MY_DIR=`dirname $0`
CHAN=ordercheck${RANDOM}${RANDOM}${RANDOM}
WRITE_CT=200
PAYLOAD_SIZE=100

echo Creating a new channel
curl -i -X POST --header "Content-type: application/json" \
    --data "{\"name\": \"$CHAN\"}"  \
    http://datahub-01.cloud-east.dev:8080/channel

echo Firing up some red speedo...
for i in `seq 1 3` ; do
    for j in `seq 1 5` ; do
        python ${MY_DIR}/red_speedo.py \
            --quiet \
            --chan http://datahub-0${i}.cloud-east.dev:8080/channel/${CHAN} \
            --count ${WRITE_CT} \
            --size ${PAYLOAD_SIZE} &
    done
done

echo Waiting for the speedo to finish
wait

echo Finding the most recent payload...
LATEST=`curl -s -q -i http://datahub-01.cloud-east.dev:8080/channel/${CHAN}/latest | grep Location | sed -e 's/.*: //' | sed -e 's/\s*$//'`
echo The latest url is \"${LATEST}\"

UNSORTED="/tmp/${CHAN}.urls"
SORTED="/tmp/${CHAN}.sorted.urls"

echo Now fetching the channel url links into ${UNSORTED}...
${MY_DIR}/traverse.sh previous ${LATEST} > ${UNSORTED}

echo Sorting...
sort -r ${UNSORTED} > ${SORTED}

echo Comparing....
diff ${UNSORTED} ${SORTED}

RESULT=$?
echo Final result: $?
exit $?