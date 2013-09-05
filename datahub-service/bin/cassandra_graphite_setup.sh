#!/bin/bash

function build_deploy_jar {
	GRAPHITE_REPORTER_JAR="graphite-reporter-${GRAPHITE_HOST}.jar"
	echo Building agent: ${GRAPHITE_REPORTER_JAR}...

	echo Checking if graphite reporter agent exists already...
	ssh ${USER}@${HOST} ls /home/${USER}/${CASSANDRA_DIR}/${GRAPHITE_REPORTER_JAR}
	if [ "$?" == "0" ] && [ "$FORCE" != "1" ]; then
		echo Graphite reporter jar ${GRAPHITE_REPORTER_JAR} already exists on ${HOST}
		return 1
	fi

	REPORTER_BUILD_DIR=/tmp/graphite-reporter-build.$$.$RANDOM
	REPORTER_CLASS_PACKAGE=${REPORTER_BUILD_DIR}/com/flightstats/datahub/metrics
	mkdir -p $REPORTER_CLASS_PACKAGE
	REPORTER_CLASS=${REPORTER_CLASS_PACKAGE}/DatahubGraphiteReportAgent.java

	cat <<EOF > ${REPORTER_CLASS}
package com.flightstats.datahub.metrics;

import com.yammer.metrics.reporting.GraphiteReporter;

import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class DatahubGraphiteReportAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        GraphiteReporter.enable(60, TimeUnit.SECONDS, "${GRAPHITE_HOST}", 2003, buildPrefix());
    }

    private static String buildPrefix() {
        try {
            return "datahub." + InetAddress.getLocalHost().getHostName().replaceFirst("\\\\..*", "");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error determining local hostname");
        }
    }
}
EOF

	mkdir -p ${REPORTER_BUILD_DIR}/META-INF
	cat <<MAN > ${REPORTER_BUILD_DIR}/META-INF/MANIFEST.MF
Manifest-Version: 1.0
Premain-Class: com.flightstats.datahub.metrics.DatahubGraphiteReportAgent
MAN

	echo Compiling reporting agent...
	javac -cp ${METRICS_CORE_LIB}:${METRICS_GRAPHITE_LIB} ${REPORTER_CLASS}
	if [ "$?" != "0" ]
	then
		echo ERROR: Compilation failure
		return 1
	fi

	pushd ${REPORTER_BUILD_DIR}
	jar -cfM ${GRAPHITE_REPORTER_JAR} .
	
	# copies jar to top level cassandra directory
	echo Rsyncing ${GRAPHITE_REPORTER_JAR} to remote host...
	rsync -avv --progress ${GRAPHITE_REPORTER_JAR} ${USER}@${HOST}:~/${CASSANDRA_DIR}/lib
	popd

	echo Rsyncing ${METRICS_GRAPHITE_LIB} to ${CASSANDRA_DIR}/lib...
	rsync -avv --progress ${METRICS_GRAPHITE_LIB} ${USER}@${HOST}:~/${CASSANDRA_DIR}/lib

	echo Checking to see if ${CASSANDRA_ENV} needs to be updated...
	ssh ${USER}@${HOST} "grep -e ${GRAPHITE_REPORTER_JAR} ${CASSANDRA_ENV}"
	if [ "$?" != "0" ]
	then
		echo Adding reporter agent to the jvm options...
		OPTS="JVM_OPTS=\"-javaagent:/home/${USER}/${CASSANDRA_DIR}/lib/${GRAPHITE_REPORTER_JAR} \$JVM_OPTS\""
		ssh ${USER}@${HOST} "echo '${OPTS}' >> ${CASSANDRA_ENV}"
	else
		echo Looks like it is all up to date!
	fi

	echo Done setting up graphite reporting!
	return 0
}

function setup_graphite {
	if [ "${USER}" == "" ]
	then
		echo USER not set
		return 1
	fi

	if [[ ${HOST} =~ \.dev$ ]]
	then
		GRAPHITE_HOST="svcsmon.cloud-east.dev"
	elif [[ ${HOST} =~ \.staging$ ]]
	then
		GRAPHITE_HOST="svcsmon.cloud-east.staging"
	elif [[ ${HOST} =~ \.prod$ ]]
	then
		GRAPHITE_HOST="svcsmon.cloud-east.prod"
	elif [ "${HOST}" == "localhost" ]
	then
		GRAPHITE_HOST="localhost"
	else
		echo Unexpected input for HOST: ${HOST}
		echo "Expected a domain ending with '.dev', '.staging', or '.prod'"
		return 1
	fi
	echo Using graphite host ${GRAPHITE_HOST}

	if [ "${CASSANDRA_DIR}" == "" ]
	then
		echo CASSANDRA_DIR not set
		return 1
	fi

	if [ "${BIN_DIR}" == "" ]
	then
		echo BIN_DIR not set
		return 1
	fi

	METRICS_LIB_DIR=${BIN_DIR}/lib
	if [ ! -d ${METRICS_LIB_DIR} ]
	then
		echo Metrics lib directory not found
		return 1
	fi

	METRICS_CORE_LIB=${METRICS_LIB_DIR}/metrics-core-2.0.3.jar
	METRICS_GRAPHITE_LIB=${METRICS_LIB_DIR}/metrics-graphite-2.0.3.jar
	if [ ! -e ${METRICS_CORE_LIB} ]
	then
		echo Metrics core library missing from ${METRICS_LIB_DIR}
		return 1
	elif [ ! -e ${METRICS_GRAPHITE_LIB} ]
    then
		echo Metrics graphite library missing from ${METRICS_LIB_DIR}
		return 1
	fi

	CASSANDRA_ENV=/home/${USER}/${CASSANDRA_DIR}/conf/cassandra-env.sh
	build_deploy_jar
	return 0
}
