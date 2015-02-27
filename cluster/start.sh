#!/bin/bash

# Exit on any error
set -e

if [ -z "${APP_NAME}" ] ; then
  APP_NAME="hub"
fi

# Let's check some assumptions!
APP_HOME=${HOME}/${APP_NAME}/
APP_LOGS=${HOME}/${APP_NAME}/log/
APP_STATE=${HOME}/${APP_NAME}/run/

# Start logging after the variables are all set
# Merge stdout and stderr
exec >> ${APP_LOGS}/${APP_NAME}.log 2>&1

# Should be running as the app-name user
if [ "$(whoami)" != "${APP_NAME}" ] ; then
  echo "Application should be running as '${APP_NAME}', aborting for safety"
  exit 2
fi

if [ ! -d ${APP_HOME} ] ; then
  echo "Application home not present : ${APP_HOME}"
  exit 3
fi

if [ ! -d ${APP_LOGS} ] ; then
  echo "Applicaion log directory not present : ${APP_LOGS} / Creating!"
  mkdir -p ${APP_LOGS}
fi

if [ ! -d ${APP_STATE} ] ; then
  echo "Applicaion state directory not present : ${APP_STATE} / Creating!"
  mkdir -p ${APP_STATE}
fi

pushd ${APP_HOME}

# HACK
find -type d -exec chmod +x {} \;

# Use the parent PID as our "ID"
ID=$$
JAVA_OPTS_FILE=${APP_STATE}/javaopts-${ID}

cat > ${JAVA_OPTS_FILE} <<JAVA_OPTS
 -d64
 -server
 -Xmx{{ max_heap }}
 -Xms{{ min_heap }}
 -XX:NewSize={{ min_new }}
 -XX:+UseG1GC
 -XX:MaxGCPauseMillis=100
 -agentlib:jdwp=transport=dt_socket,address=3333,server=y,suspend=n

 -Dcom.sun.management.jmxremote
 -Dcom.sun.management.jmxremote.port=8888
 -Dcom.sun.management.jmxremote.ssl=false
 -Dcom.sun.management.jmxremote.authenticate=false
 -Dlogback.configurationFile=${APP_HOME}/conf/logback.xml
 -Dnewrelic.environment={{ environment }}

 -XX:+PrintGCDetails
 -XX:+PrintTenuringDistribution
 -XX:+PrintGCDateStamps

 -Dsun.net.inetaddr.ttl=0

 -Xloggc:${APP_LOGS}/hub_gc.log-${ID}-$(date -u '+%Y-%m-%d-%H-%M-%S')
JAVA_OPTS

JAVA_OPTS="$(cat ${JAVA_OPTS_FILE})"

MAIN_CLASS="com.flightstats.hub.app.HubMain"

JAVA_AGENT="-javaagent:$(ls ${APP_HOME}lib/newrelic-agent*.jar)"

CLI="java -cp $(ls ${APP_HOME}lib/hub-*):${APP_HOME}lib/* ${JAVA_AGENT} ${JAVA_OPTS} ${MAIN_CLASS} ${APP_HOME}/conf/hub.properties"

set -f
echo "Running : ${CLI}"
${CLI}
