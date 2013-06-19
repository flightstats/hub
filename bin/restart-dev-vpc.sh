#!/bin/bash

#
# This starts up the 3 dev data-hub VPC instances.
# It uses curl and throws data at http://deploy.util.hq.prod:8082/

URL="http://deploy.util.hq.prod:8082/"
OWNER='jason.plumb@flightstats.com'
OS=`uname`

INSTANCES=( \
	cassandra-01.i-9e3a5ded:9160 \
    cassandra-02.i-c0395eb3:9160 \
    cassandra-03.i-9c395eef:9160 \
	datahub-01.i-b4572ac4:22 \
	datahub-02.i-52562b22:22 \
	datahub-03.i-22562b52:22
)

if [ -x /usr/local/bin/nc ] ; then
    NC=/usr/local/bin/nc
elif [ -x /usr/bin/nc ] ; then
    NC=/usr/bin/nc
elif [ -x /bin/nc ] ; then
    NC=/bin/nc
else
    echo "Cannot find a usable netcat (nc)"
    exit 1
fi

function port_check {
    HOST=$1
    PORT=$2
    $NC -z -w 1 ${HOST} ${PORT}
}

function wait_for_host {
    HOSTNAME=$1
    PORT=$2
    echo Waiting for ${HOSTNAME}:${PORT} to be up...
    while :
    do
        port_check ${HOSTNAME} ${PORT}
        if [ "$?" == "0" ]; then
            echo ${HOSTNAME} is alive.
            break
        else
            echo -n ...zzz...
            sleep 1
            echo -n -e "\b\b\b\b\b\b\b\b\b"
        fi
    done
}

for INSTANCE in ${INSTANCES[@]}; do
	echo '-------------------------------------------------------------------'
	HOSTNAME=`echo ${INSTANCE} | sed -e "s/\..*//"`.cloud-east.dev
	PORT=`echo ${INSTANCE} | sed -e "s/^.*://"`

	echo "Checking to see if ${HOSTNAME} is alive..."
	port_check ${HOSTNAME} ${PORT}
	if [ "$?" == "0" ]; then
		echo ${HOSTNAME} is already running!
	else
		INSTANCE_ID=`echo ${INSTANCE} | sed -e "s/^.*\.//" | sed -e "s/:.*//"`
		RESTART_URL="http://deploy.util.hq.prod:6543/vpc/${INSTANCE_ID}/restart"
		echo Resurrecting "${HOSTNAME} (instance = ${INSTANCE_ID})"
		curl -s ${RESTART_URL} &
		echo
		wait_for_host ${HOSTNAME} ${PORT}
	fi
done

echo
echo '-------------------------------------------------------------------'
echo '     _  _'
echo '    ( `   )_    All done.'
echo '   (    )    `)   Enjoy the cloud.'
echo ' (_   (_ .  _) _)'
