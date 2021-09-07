FROM openjdk:8

RUN apt-get update && apt-get install -y bash ntp curl && \
    mkdir -p /mnt/log && \
    mkdir -p /mnt/spoke/write \
    mkdir -p /mnt/spoke/read

ENV MIN_HEAP=1g \
    MAX_HEAP=3g \
    NEW_SIZE=100m

ADD configs /etc/hub

RUN groupadd -g 2001 hub
RUN useradd -u 2001 -g 2001 hub
USER hub

ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chmod +x /opt/hub/bin/*

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
