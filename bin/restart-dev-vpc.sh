#!/bin/bash

#
# This starts up the 3 dev data-hub VPC instances.
# It uses curl and throws data at http://deploy.util.hq.prod:8082/

URL="http://deploy.util.hq.prod:8082/"
OWNER='jason.plumb@flightstats.com'
OS=`uname`

INSTANCES=( \
	cassandra-01.i-9e3a5ded \
    cassandra-02.i-c0395eb3 \
    cassandra-03.i-9c395eef \
	datahub-01.i-b4572ac4 \
) 

function ping_host {
    HOSTNAME=$1
	if [ "$OS" == "Darwin" ] ; then
	    ping -q -c 1 -t 2 ${HOSTNAME} > /dev/null
	else
	    ping -q -c 1 -W 2 ${HOSTNAME} > /dev/null
	fi
}

for INSTANCE in ${INSTANCES[@]}; do
	echo '-------------------------------------------------------------------'
	HOSTNAME=`echo ${INSTANCE} | sed -e "s/\..*//"`.cloud-east.dev

	echo "Checking to see if ${HOSTNAME} is alive..."
	ping_host ${HOSTNAME}
	if [ "$?" == "0" ]; then
		echo ${HOSTNAME} is already running!
	else
		INSTANCE_ID=`echo ${INSTANCE} | sed -e "s/^.*\.//"`
		RESTART_URL="http://deploy.util.hq.prod:6543/vpc/${INSTANCE_ID}/restart"
		echo Resurrecting "${HOSTNAME} (instance = ${INSTANCE_ID})"
		curl -s ${RESTART_URL} &
		echo 
	fi
done

echo '-------------------------------------------------------------------'
echo Waiting for all instances to be alive...
for INSTANCE in ${INSTANCES[@]}; do
	HOSTNAME=`echo ${INSTANCE} | sed -e "s/\..*//"`.cloud-east.dev
	while :
	do
	    ping_host ${HOSTNAME}
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
