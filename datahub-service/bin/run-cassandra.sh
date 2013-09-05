#!/bin/bash

# This is a unix start script for cassandra.

CASSANDRA_DIR=/home/ubuntu/apache-cassandra-1.2.9
USER=ubuntu
PIDFILE=${CASSANDRA_DIR}/cassandra.pid

touch ${PIDFILE}
chown ${USER}:${USER} ${PIDFILE}

start-stop-daemon --start \
    --chuid ${USER}:${USER} \
    --chdir ${CASSANDRA_DIR} \
    --pidfile ${PIDFILE} \
    --exec /bin/bash \
    -- --login -c "${CASSANDRA_DIR}/bin/cassandra -p ${PIDFILE} -f"
