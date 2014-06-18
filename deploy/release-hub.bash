#!/bin/bash

# Temporary rolling-deploy script until triforce.deploy can handle multiple targets and return debug/warn/info to jenkins

VERSION="hub-${2}.tgz"
ENV=${1:-dev}
VER_NUM1=${VERSION:4}
VER_NUM=${VER_NUM1%.tgz}
SERVERS=3

echo "Deploying to ${ENV} : ${VERSION}"
case ${ENV} in 
	dev)
	    DOM=".cloud-east.dev"
	    PREFIX="hub"
	     ;;
	encrypted-dev)
	    DOM=".cloud-east.dev"
	    PREFIX="encrypted-hub"
	     ;;
	staging)
	    DOM=".cloud-east.staging"
	    PREFIX="hub"
	     ;;
    encrypted-staging)
	    DOM=".cloud-east.staging"
	    PREFIX="encrypted-hub"
	     ;;
	prod)
	    DOM=".cloud-east.prod"
	    PREFIX="hub"
	    ;;
	data-qa-staging)
	    DOM=".cloud-east.staging"
	    PREFIX="hub-data-qa"
	    SERVERS=2
	    ;;
	*) 
		echo "No env specified or bad env ${ENV}" ; exit ;;
esac
	
# cat newest file in /tmp/triforce:
function node_out {
	ssh utility@saltmaster01.util.pdx.office 'sudo salt '${SERVER}' cmd.run "cat /tmp/triforce/\$(ls -t /tmp/triforce/ | head -1)"'
}

for n in {1..${SERVERS}}; do
	SERVER="${PREFIX}-0${n}${DOM}"
	echo "Calling ${SERVER}"

	# send salt-call (or ssh to salt master) to deploy
	salt_output=$(ssh utility@saltmaster01.util.pdx.office "sudo salt '${SERVER}' triforce.deploy s3://triforce_builds/hub/${VERSION} ${ENV}")
	echo $salt_output
	# if version in salt_output contains ${VER_NUM}, we're good. if not, exit and give jenkins all the return data
	if [[ $salt_output == *"$VER_NUM"* ]]
	then
		node_out | grep INFO
		echo "${SERVER} deployed successfully."
	else
		node_out
		echo "${SERVER} deployment failed."
		exit 1
	fi
done
echo "All nodes deployed."