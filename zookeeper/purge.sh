#!/bin/sh
#should run this every 5 minutes
DATA_DIR=/ebs/zookeeper/version-2/

echo "removing files from ${DATA_DIR}"
cd ${DATA_DIR}
rm -v `ls -t | grep log | awk 'NR>3'`
rm -v `ls -t | grep snapshot | awk 'NR>3'`
