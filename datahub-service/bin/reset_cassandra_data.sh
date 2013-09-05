#!/bin/bash

# This script will do the following on each of the given hosts:
# 1) stop cassandra
# 2) delete the data directory
# 3) start cassandra

USER=ubuntu
DATA_DIR=/mnt/cassandra/data

function usage {
    echo "Usage: ${0} <host> ... <host>"
}

if [ "$#" == "0" ] ; then
    usage
    exit 1
fi

for HOST in $@ ; do
    echo Stopping cassandra on ${HOST}...
    ssh ${USER}@${HOST} "sudo stop cassandra"
done

for HOST in $@ ; do
    echo Purging data directory on ${HOST}...
    ssh ${USER}@${HOST} "rm -rf ${DATA_DIR}/*"
done

for HOST in $@ ; do
    echo Starting cassandra on ${HOST}...
    ssh ${USER}@${HOST} "sudo start cassandra"
done

echo All done.