#!/bin/bash
### BEGIN INIT INFO
# Provides:          datahub
# Required-Start:    $local_fs $remote_fs $syslog
# Required-Stop:     
# Should-Start:      cassandra
# Should-Stop:       
# X-Start-Before:    
# X-Stop-After:      
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: FlightStats data hub
# Description:       FlightStats data hub
### END INIT INFO

DATAHUB_DIR=/home/ubuntu/datahub
PIDFILE=${DATAHUB_DIR}/datahub.pid
USER=ubuntu

case "$1" in
	start)
		echo Starting datahub instance
		touch ${PIDFILE}
		chown ubuntu:ubuntu ${PIDFILE}

		start-stop-daemon --start \
			--chuid ${USER}:${USER} \
			--chdir ${DATAHUB_DIR} \
			--pidfile ${PIDFILE} \
			--make-pidfile \
			--background \
			--exec /bin/bash \
			-- --login -c "${DATAHUB_DIR}/bin/datahub"
		;;
	stop)
		echo Stopping datahub instance
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
