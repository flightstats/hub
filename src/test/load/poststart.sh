#!/bin/sh

# http://hub-node-tester.cloud-east.dev:8089/swarm
# locust_count:20
# hatch_rate:1

sleep 2
curl --data "locust_count=5&hatch_rate=1" http://localhost:8089/swarm