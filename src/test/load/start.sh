#!/bin/sh

CLI="locust -f /home/ubuntu/load/read-write-group.py -H http://hub-v2.svc.dev"

set -f
echo "Running : ${CLI}"
${CLI}