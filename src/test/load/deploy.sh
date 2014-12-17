#!/bin/sh

SERVER=${1:-hub-node-tester.cloud-east.dev}
APP_PATH=/home/ubuntu/load
FILE_PATH=${2:-.}

scp ${FILE_PATH}/*.py ubuntu@${SERVER}:${APP_PATH}/.
scp ${FILE_PATH}/*.sh ubuntu@${SERVER}:${APP_PATH}/.
ssh ubuntu@${SERVER} "chmod +x ${APP_PATH}/*.sh"
ssh ubuntu@${SERVER} "sudo service hub-locust restart"


