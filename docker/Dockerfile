FROM java:8

RUN apt-get update && apt-get install -y bash ntp curl

RUN mkdir -p /mnt/log
RUN mkdir -p /mnt/spoke
RUN mkdir -p /tmp/storage

ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chmod +x /opt/hub/bin/*

ADD logback.xml /etc/hub/logback.xml
ADD hub.properties /etc/hub/hub.properties

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
