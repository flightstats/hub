FROM openjdk:8

RUN apt-get update && apt-get install -y bash ntp curl

RUN mkdir -p /mnt/log
RUN mkdir -p /mnt/spoke
RUN mkdir -p /tmp/storage

ADD logback.xml /etc/hub/logback.xml
ADD hub.properties /etc/hub/hub.properties

ENV MIN_HEAP=1g \
    MAX_HEAP=2g \
    NEW_SIZE=100m

ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chmod +x /opt/hub/bin/*

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
