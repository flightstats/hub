FROM amazoncorretto:8

RUN yum update -y && yum install -y bash ntp curl && \
    mkdir -p /mnt/log && \
    mkdir -p /mnt/spoke/write \
    mkdir -p /mnt/spoke/read

ENV MIN_HEAP=1g \
    MAX_HEAP=3g \
    NEW_SIZE=100m

ADD configs /etc/hub


ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chmod +x /opt/hub/bin/*

ADD config.yaml /opt/hub/config.yaml

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
