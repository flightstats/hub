#!/bin/bash

function usage {
	echo "Usage:"
	echo "`basename ${0}` -h <host> -s <seed> [-f]"
	echo ""
	echo " where "
	echo "   -h <host>        - the host to install/configure cassandra on"
	echo "   -s <seed>        - the seed host"
	echo "   -f               - force overwrite of cassandra install"
	echo ""
	echo " Preconditions/Notes/Assumptions: "
	echo " - The host must be running"
	echo " - You must have an ssh key set up for this host"
	echo " - The host must be able to resolve its own name to an IP address."
}


while getopts "fh:s:m:" opt; do
	case $opt in 
		h)
			HOST=$OPTARG
			;;
		s)
			SEED_HOST=$OPTARG
			;;
		f)
			FORCE=1
			;;
		\?)
			usage
			exit 1
			;;
	esac
done

if [ "$HOST" == "" ] || [ "$SEED_HOST" == "" ] || [ "$#" == "0" ]; then
	usage
	exit 1
fi

USER=ubuntu
CASSANDRA_TGZ_URL=http://apache.osuosl.org/cassandra/1.2.1/apache-cassandra-1.2.1-bin.tar.gz
CASSANDRA_TGZ=`basename ${CASSANDRA_TGZ_URL}`
LOCAL_CASSANDRA_TGZ=/tmp/${CASSANDRA_TGZ}
CASSANDRA_DIR=`basename ${CASSANDRA_TGZ} '-bin.tar.gz'`
BIN_DIR=`dirname ${0}`
CONF_DIR=${BIN_DIR}/../conf

echo
echo Got it.  I like your style.
echo Ok, let\'s set up a ${MODE} cassandra node on ${HOST}
echo

echo Cassandra requires Java...let\'s make sure we have a version we like...
${BIN_DIR}/install_java.sh ${HOST}

echo Checking to see if remote already has a cassandra install...
ssh ${USER}@${HOST} ls -d /home/${USER}/${CASSANDRA_DIR}

if [ "$?" != "0" ] || [ "$FORCE" == "1" ]; then
	echo "Remote cassandra doesn't exist or -f given..."
	pushd .
	cd /tmp
	echo Downloading cassandra
	wget -nc "${CASSANDRA_TGZ_URL}"
	popd
	echo Pushing cassandra to ${HOST}...
	rsync -avv --progress ${LOCAL_CASSANDRA_TGZ} ${USER}@${HOST}:~
	echo Extracting cassandra tarball...
	ssh ${USER}@${HOST} tar -xzf ${CASSANDRA_TGZ}
else
	echo Looks like the cassandra dir is already there.
fi

echo Filling out cassandra configuration template...
cat ${CONF_DIR}/cassandra.template.yaml | \
	sed -e "s/##SEEDS##/${SEED_HOST}/" | \
	sed -e "s/##LISTEN_ADDRESS##/${HOST}/" > /tmp/cassandra.yaml

echo Uploading cassandra configuration file...
rsync -a /tmp/cassandra.yaml ${USER}@${HOST}:~/${CASSANDRA_DIR}/conf/
echo Uploading log4j properties...
rsync -a ${CONF_DIR}/log4j-server.properties ${USER}@${HOST}:~/${CASSANDRA_DIR}/conf/

echo Making cassandra data directory
ssh ${USER}@${HOST} "sudo mkdir -p /mnt/cassandra/data"
ssh ${USER}@${HOST} "sudo chown ${USER}:${USER} /mnt/cassandra/data"

echo Making cassandra commitlog directory
ssh ${USER}@${HOST} "sudo mkdir -p /mnt/cassandra/commitlog"
ssh ${USER}@${HOST} "sudo chown ${USER}:${USER} /mnt/cassandra/commitlog"

echo Uploading cassandra logging configuration...
rsync -a ${CONF_DIR}/log4j-server.properties ${USER}@${HOST}:~/${CASSANDRA_DIR}/conf/

# Kinda bold and presumptious to be doing this here...but hmmmmph.
echo Installing JNA so that native code will run from Java...
ssh ${USER}@${HOST} 'sudo apt-get install libjna-java'
echo Symlinking jna.jar into cassandra/lib directory
ssh ${USER}@${HOST} "ln -s /usr/share/java/jna.jar /home/${USER}/${CASSANDRA_DIR}/lib/jna.jar"

echo Installing init/startup scripts...
rsync -a ${BIN_DIR}/cassandra-init.sh ${USER}@${HOST}:/tmp/
ssh ${USER}@${HOST} 'sudo cp /tmp/cassandra-init.sh /etc/init.d/'
ssh ${USER}@${HOST} 'sudo ln -s /etc/init.d/cassandra-init.sh /etc/init.d/cassandra'
ssh ${USER}@${HOST} 'sudo ln -s /etc/init.d/cassandra /etc/rc2.d/S99cassandra'

echo Starting up cassandra on ${HOST}
ssh ${USER}@${HOST} "sudo /etc/init.d/cassandra start"

# TODO: Initial tokens in cassandra.yaml ?
