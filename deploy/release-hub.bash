#!/bin/bash

# Temporary rolling-deploy script until triforce.deploy can handle multiple targets and return debug/warn/info to jenkins

VERSION="hub-v2-${2}.tgz"
ENV=${1:-dev}
VER_NUM1=${VERSION:4}
VER_NUM=${VER_NUM1%.tgz}
SERVERS=3
TYPE=${3:-deploy}

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
	prod)
	    DOM=".cloud-east.prod"
	    PREFIX="hub"
	    ;;
	integration)
	    DOM=".cloud-east.staging"
	    PREFIX="hub-int"
	    ;;
	*)
		echo "No env specified or bad env ${ENV}" ; exit ;;
esac
	
# cat newest file in /tmp/triforce:
function node_out {
	ssh utility@saltmaster01.util.pdx.office 'sudo salt '${SERVER}' cmd.run "cat /tmp/triforce/\$(ls -t /tmp/triforce/ | head -1)"'
}

function deploy {
    # send salt-call (or ssh to salt master) to deploy
	salt_output=$(ssh utility@saltmaster01.util.pdx.office "sudo salt '${SERVER}' triforce.deploy s3://triforce_builds/hubv2/${VERSION} ${ENV}")
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
}

function restart {
    ssh utility@${SERVER} "sudo service hub restart"

    jasmine-node --captureExceptions --config host ${SERVER} ./deploy/wait_for_health_spec.js
    if [ $? -eq 0 ]
    then
      echo "success!"
    else
      echo "unable to restart ${SERVER}.  exiting"
      exit 1
    fi

}
for n in $( seq 1 ${SERVERS} ) ; do
	SERVER="${PREFIX}-0${n}${DOM}"
	echo "Calling ${SERVER}"

    case ${TYPE} in
        deploy)
            deploy
            ;;
        restart)
            restart
            ;;
        *)
            echo "invalid type ${TYPE}" ; exit ;;
    esac
done
echo "All nodes ${TYPE}ed"
