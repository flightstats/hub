#!/bin/bash
# 
# Simple deploy script
#

HOST="$1"
USER=ubuntu
BIN_DIR=`dirname $0`
CONF_DIR=${BIN_DIR}/../conf
BUILD_DIR=${BIN_DIR}/../build
TARFILE=`ls ${BUILD_DIR}/distributions/datahub-*.tgz`
TARFILE=`basename ${TARFILE}`
DISTDIR=`basename ${TARFILE} .tgz`

if [ "$HOST" == "" ] ; then
	echo "Usage: $0 <host>"
	exit 1
fi

echo Shutting down any running datahub instances on ${HOST}...
ssh ${USER}@${HOST} "sudo /etc/init.d/datahub stop"

echo Deploying ${TARFILE} to ${HOST}
rsync -avv --progress ${BUILD_DIR}/distributions/${TARFILE} ${USER}@${HOST}:/home/${USER}/

echo Exploding tarball...
ssh ${USER}@${HOST} "tar -xzf /home/${USER}/${TARFILE}"

echo Creating symlink
ssh ${USER}@${HOST} "rm /home/${USER}/datahub; ln -s /home/${USER}/${DISTDIR} /home/${USER}/datahub"

echo Installing properties file
if [[ "$HOST" == *dev* ]] ; then
	rsync -avv --progress ${CONF_DIR}/dev/datahub.properties ${USER}@${HOST}:/home/${USER}/datahub/
elif [[ "$HOST" == *staging* ]] ; then
	rsync -avv --progress ${CONF_DIR}/dev/datahub.properties ${USER}@${HOST}:/home/${USER}/datahub/
fi

echo Installing init script
rsync -avv --progress ${BIN_DIR}/datahub-init.sh ${USER}@${HOST}:/tmp
ssh ${USER}@${HOST} "sudo mv /tmp/datahub-init.sh /etc/init.d/"
ssh ${USER}@${HOST} "sudo ln -s /etc/init.d/datahub-init.sh /etc/init.d/datahub"
ssh ${USER}@${HOST} "sudo ln -s /etc/init.d/datahub /etc/rc2.d/S99datahub"

echo Starting up datahub...
ssh ${USER}@${HOST} "sudo /etc/init.d/datahub start"

echo Waiting for service to be active...
for i in `seq 1 60` ; do
	curl --silent http://${HOST}:8080 > /dev/null
	if [ "$?" == "0" ] ; then
		echo Service is up.
		exit 0
	fi
	echo -n .
	sleep 1
done
echo Timeout waiting for service to start.
exit 1

