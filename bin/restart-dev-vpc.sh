#!/bin/bash

#
# This starts up the 3 dev data-hub VPC instances.
# It uses curl and throws data at http://deploy.util.hq.prod:8082/

URL="http://deploy.util.hq.prod:8082/"
OWNER='jason.plumb@flightstats.com'

INSTANCE_IDS=( \
	datahub-01.i-b4572ac4.ec2 \
	datahub-02.i-52562b22.ec2 \ 
	datahub-03.i-22562b52.ec2 \
) 

for INSTANCE in ${INSTANCE_IDS[@]}; do
	echo '-------------------------------------------------------------------'
	HOSTNAME=`echo ${INSTANCE} | sed -e "s/\..*//"`.cloud-east.dev
	echo "Checking to see if ${HOSTNAME} is alive..."
	ping -q -c 1 -t 2 ${HOSTNAME} > /dev/null
	if [ "$?" == "0" ]; then
		echo ${HOSTNAME} is already running!
	else
		echo Resurrecting "${HOSTNAME} (instance = ${INSTANCE})"
		curl ${URL} -d OWNER=${OWNER} -d INSTANCE_FILE=${INSTANCE}
		echo 
	fi
done
