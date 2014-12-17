#!/bin/sh

SERVER=${1:-hub-node-tester.cloud-east.dev}
APP_PATH=/home/ubuntu/load
FILE_PATH=${2:-.}

rsync -av ${FILE_PATH}/*.py ubuntu@${SERVER}:${APP_PATH}/.
rsync -av ${FILE_PATH}/*.sh ubuntu@${SERVER}:${APP_PATH}/.
ssh ubuntu@${SERVER} "chmod +x ${APP_PATH}/*.sh"
ssh ubuntu@${SERVER} "sudo service hub-locust restart"


