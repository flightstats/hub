#!/bin/sh

HOST=`hostname`

case ${HOST} in
	hub-node-tester.cloud-east.dev)
	    HUB="http://hub-v2.svc.dev"
	     ;;
	hub-node-tester.cloud-east.staging)
	    HUB="http://hub-v2-int.svc.staging"
	     ;;
	*)
		echo "host not supported ${HOST}" ; exit ;;
esac

CLI="locust -f /home/ubuntu/load/read-write-group.py -H ${HUB}"

set -f
echo "Running : ${CLI}"
${CLI}