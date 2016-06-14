#!/bin/sh

sleep 2
curl --data "locust_count=5&hatch_rate=1" http://localhost:8089/swarm