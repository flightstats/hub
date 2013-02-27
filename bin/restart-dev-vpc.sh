#!/bin/bash

#
# This starts up the 3 dev data-hub VPC instances.
# It uses curl and throws data at http://deploy.util.hq.prod:8082/

URL="http://deploy.util.hq.prod:8082/"
OWNER='jason.plumb@flightstats.com'

INSTANCE_IDS=( \
	datahub-01.i-b4572ac4.ec2 \
	cassandra-01.i-9e3a5ded.ec2 \
    cassandra-02.i-c0395eb3.ec2 \ 
    cassandra-03.i-9c395eef.ec2 \
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

echo '-------------------------------------------------------------------'
echo Waiting for all instances to be alive...
for INSTANCE in ${INSTANCE_IDS[@]}; do
	HOSTNAME=`echo ${INSTANCE} | sed -e "s/\..*//"`.cloud-east.dev
	while :
	do
		ping -q -c 1 -t 2 ${HOSTNAME} > /dev/null
		if [ "$?" == "0" ]; then
			echo ${HOSTNAME} is alive.
			break
		else
			echo -n ...zzz...
			sleep 1
			echo -n -e "\b\b\b\b\b\b\b\b\b"
		fi
	done
done

echo '     _  _'
echo '    ( `   )_    All done.'
echo '   (    )    `)   Enjoy the cloud.'
echo ' (_   (_ .  _) _)'
