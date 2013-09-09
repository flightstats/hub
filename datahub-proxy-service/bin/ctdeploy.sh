#!/bin/bash
DESTHOST=$1

BUILD_DIR="$(dirname $0)/../build"

if [ -z "$DESTHOST" ] ; then 
		echo "ctdeploy.sh <DESTINATION HOST>"
		exit 1
fi

cat ${BUILD_DIR}/distributions/datahub-proxy-service-*.tgz | ssh ctdeploy@${DESTHOST} deploy_stdin
