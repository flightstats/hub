#!/bin/bash

#
# This is a unix init script for cassandra.
#

CASSANDRA_DIR=/home/ubuntu/apache-cassandra-1.2.1
PIDFILE=${CASSANDRA_DIR}/cassandra.pid
USER=ubuntu

case "$1" in
	start)

	    if [ ! -d /mnt/cassandra ] ; then
            mkdir /mnt/cassandra
            mkdir /mnt/cassandra/commitlog
            mkdir /mnt/cassandra/data

            chown ubuntu:ubuntu /mnt/cassandra
            chown ubuntu:ubuntu /mnt/cassandra/commitlog
            chown ubuntu:ubuntu /mnt/cassandra/data
        fi

		touch ${PIDFILE}
		chown ubuntu:ubuntu ${PIDFILE}

		start-stop-daemon --start \
			--chuid ${USER}:${USER} \
			--chdir ${CASSANDRA_DIR} \
			--pidfile ${PIDFILE} \
			--exec /bin/bash \
			-- --login -c "${CASSANDRA_DIR}/bin/cassandra -p ${PIDFILE}"
		;;
	stop)
		start-stop-daemon --stop \
			--pidfile ${PIDFILE} \
			--user ${USER} \
			--retry TERM/30/KILL/5
		;;
	*)
		echo "Usage: $0 start|stop" >&2
		exit 1
		;;
esac
