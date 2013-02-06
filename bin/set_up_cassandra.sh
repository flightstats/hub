#!/bin/bash

function usage {
	echo "Usage:"
	echo "`basename ${0}` -m (seed|node) -h <host> -s <seeds> [-f]"
	echo ""
	echo " where "
	echo "   -m <seed|node>   - \"mode\", either \"seed\" or \"node\""
	echo "   -h <host>        - the host to install/configure cassandra on"
	echo "   -s <seed>        - the seed host"
	echo "   -f               - force overwrite of cassandra install"
	echo ""
	echo " Preconditions/Notes/Assumptions: "
	echo " - The host must be running"
	echo " - You must have an ssh key set up for this host"
	echo " - The host must be able to resolve its own name to an IP address."
}


while getopts "h:s:m:" opt; do
	case $opt in 
		m)
			MODE=$OPTARG
			;;
		h)
			HOST=$OPTARG
			;;
		s)
			SEEDS=$OPTARG
			;;
		\?)
			usage
			exit 1
			;;
	esac
done

if [ "$MODE" != "seed" ] && [ "$MODE" != "node" ] ; then
	usage
	echo "mode -m must be one of \"seed\" or \"node\""
	exit 1
fi
if [ "$HOST" == "" ] || [ "$SEEDS" == "" ] || [ "$#" == "0" ]; then
	usage
	exit 1
fi

USER=ubuntu
CASSANDRA_TGZ_URL=http://apache.osuosl.org/cassandra/1.2.1/apache-cassandra-1.2.1-bin.tar.gz
CASSANDRA_TGZ=/tmp/`basename ${CASSANDRA_TGZ_URL}`

echo
echo Got it.  I like your style.
echo Ok, let\'s set up a ${MODE} cassandra node on ${HOST}
echo

pushd .
cd /tmp
echo Downloading cassandra
wget -nc "${CASSANDRA_TGZ_URL}"
popd
echo Pushing cassandra to ${HOST}...
scp ${CASSANDRA_TGZ} ${USER}@${HOST}:~

# TODO: Initial tokens in cassandra.yaml ?
