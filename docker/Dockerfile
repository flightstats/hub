FROM openjdk:8

RUN apt-get update && apt-get install -y bash ntp curl && \
    mkdir -p /mnt/log && \
    mkdir -p /mnt/spoke/write \
    mkdir -p /mnt/spoke/read

ENV MIN_HEAP=1g \
    MAX_HEAP=3g \
    NEW_SIZE=100m
# Download the JAR file from the specified URL and place it in /opt
#RUN wget -O /opt/jmx_prometheus_javaagent-1.0.1.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar
#RUN chmod +x /opt/jmx_prometheus_javaagent-1.0.1.jar

ADD jmx_prometheus_javaagent-1.0.1.jar /opt/hub/jmx_prometheus_javaagent-1.0.1.jar
RUN chmod +x /opt/hub/jmx_prometheus_javaagent-1.0.1.jar
ADD config.yaml /opt/hub/config.yaml

ADD configs /etc/hub

ADD hub /opt/hub
ADD run /opt/hub/bin/run
RUN chmod +x /opt/hub/bin/*

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]
