FROM openjdk:8

RUN apt-get update && apt-get install -y bash ntp curl && \
    mkdir -p /mnt/log && \
    mkdir -p /mnt/spoke/write \
    mkdir -p /mnt/spoke/read

ENV MIN_HEAP=1g \
    MAX_HEAP=3g \
    NEW_SIZE=100m

ADD configs /etc/hub

ARG USER_ID
ARG USER_NAME

RUN groupadd -g $USER_ID $USER_NAME
RUN useradd -u $USER_ID -g $USER_ID -s /bin/bash $USER_NAME

ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chown -Rv $USER_NAME:$USER_NAME /opt/hub
RUN chown -Rv $USER_NAME:$USER_NAME /etc/hub/hub.properties
RUN chown -Rv $USER_NAME:$USER_NAME /etc/hub/logback.xml
RUN chown -Rv $USER_NAME:$USER_NAME /mnt/spoke
RUN chmod 770 -Rv /opt/hub/bin
USER $USER_NAME



ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
