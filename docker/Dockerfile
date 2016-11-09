FROM java:8

RUN apt-get update && apt-get install -y bash ntp curl

RUN mkdir -p /mnt/log && \
    mkdir -p /mnt/spoke

ADD hub /opt/hub

ADD run /opt/hub/bin/run

ADD logback.xml /etc/hub/logback.xml

# singleHubMain uses 80, hubMain uses 8080 and is assumed to be behind a load-balancer
# 3333 is the debug port and 8888 is the jmx remote port.
EXPOSE 80 8080 3333 8888

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]

# In the runfile: Java Main Class, Min heap, Max heap, Min new size.
# Loads singleHubMain by default, and we start with modest memory settings.
CMD ["com.flightstats.hub.app.SingleHubMain", "256m", "512m", "10m"]
