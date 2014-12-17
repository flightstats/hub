#!/bin/sh

scp *.py ubuntu@hub-node-tester.cloud-east.dev:load/.
scp *.sh ubuntu@hub-node-tester.cloud-east.dev:load/.
ssh ubuntu@hub-node-tester.cloud-east.dev "chmod +x load/*.sh"
ssh gmoulliet@hub-node-tester.cloud-east.dev "sudo service hub-locust restart"

