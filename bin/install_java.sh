#!/bin/bash

# Installs a known/expected version of java into the home dir and
# sets up JAVA_HOME and the PATH environment vars.

USER=ubuntu

HOST=$1
WANTED_VERSION=1.7.0_11
WANTED_VERSION_STRING="java version \"${WANTED_VERSION}\""
JRE_URL=http://artifactory.office/artifactory/services-dev/jre-7u11-linux-x64.tgz
JRE_TGZ=`basename ${JRE_URL}`

if [ "$HOST" == "" ]; then
	echo "Usage: $0 <host>"
	exit 1
fi

echo Checking existing java version on remote...
REMOTE_VERSION=`ssh ${USER}@${HOST} "source ~/.profile; java -version" 2>&1 | head -1`
echo The remote has ${REMOTE_VERSION}

if [ "${REMOTE_VERSION}" == "${WANTED_VERSION_STRING}" ]; then
	echo Remote java is already up to date!
	exit 1
fi

echo We are installing ${WANTED_VERSION_STRING}, so here goes:
echo "Grabbing JRE tarball to local file..."
pushd .
cd /tmp
wget -nc ${JRE_URL}
popd .

echo Pushing JRE to remote host...
rsync -a --progress /tmp/${JRE_TGZ} ${USER}@${HOST}:~/

echo Exploding tarball on remote host...
ssh ${USER}@${HOST} "tar -xf ~/${JRE_TGZ}"

echo Setting up the .profile
ssh ${USER}@${HOST} "echo \"JAVA_HOME=~/jre${WANTED_VERSION}\" >> ~/.profile ; echo \"export JAVA_HOME\" >> ~/.profile"
ssh ${USER}@${HOST} "echo \"PATH=~/jre${WANTED_VERSION}/bin:\\\$PATH\" >> ~/.profile ; echo \"export PATH\" >> ~/.profile"

echo Verifying the java version now...
REMOTE_VERSION=`ssh ${USER}@${HOST} "source ~/.profile; java -version" 2>&1 | head -1`
echo Remote is now ${REMOTE_VERSION}
if [ "${REMOTE_VERSION}" == "${WANTED_VERSION_STRING}" ]; then
	echo Success!
	exit 0
fi

echo Something went wrong.  FAILBOAT.
exit 1
	
