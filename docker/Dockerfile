FROM openjdk:8

RUN apt-get update && apt-get install -y bash ntp curl && \
    mkdir -p /mnt/log && \
    mkdir -p /mnt/spoke && \
    adduser --home /opt/hub --uid 4242 --shell /bin/bash --disabled-password --gecos "Hub4Docker" hub && \
    chown -R hub:hub /mnt


ENV MIN_HEAP=1g \
    MAX_HEAP=2g \
    NEW_SIZE=100m


ADD logback.xml /etc/hub/logback.xml
ADD hub.properties /etc/hub/hub.properties

ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chown -R hub:hub /opt/hub && \
    chmod +x /opt/hub/bin/*

USER hub

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
