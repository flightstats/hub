#!/bin/sh

# http://hub-node-tester.cloud-east.dev:8089/swarm
# locust_count:20
# hatch_rate:1

HOST=`hostname`

case ${HOST} in
	hub-node-tester.cloud-east.dev)
	    COUNT=5
	     ;;
	hub-node-tester.cloud-east.staging)
	    COUNT=5
	     ;;
	*)
		echo "host not supported ${HOST}" ; exit ;;
esac

sleep 2
curl --data "locust_count=${COUNT}&hatch_rate=1" http://localhost:8089/swarm