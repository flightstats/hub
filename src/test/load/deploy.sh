#!/bin/sh

SERVER=${1:-hub-node-tester.cloud-east.dev}
APP_PATH=/home/ubuntu/load

scp *.py ubuntu@${SERVER}:${APP_PATH}/.
scp *.sh ubuntu@${SERVER}:${APP_PATH}/.
ssh ubuntu@${SERVER} "chmod +x ${APP_PATH}/*.sh"
ssh ubuntu@${SERVER} "sudo service hub-locust restart"


