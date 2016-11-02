FROM java:8

RUN apt-get update && apt-get install -y bash ntp curl

ADD run /opt/hub/bin/run

ADD hub /opt/hub

ADD logback.xml /etc/hub/logback.xml

RUN chmod +x /opt/hub/bin/*

RUN mkdir -p /mnt/log && mkdir -p /mnt/spoke

# singleHubMain uses 80, hubMain uses 8080 and is assumed to be behind a load-balancer
# 3333 is the debug port and 8888 is the jmx remote port.
EXPOSE 80 8080 3333 8888

ENTRYPOINT ["/bin/bash", "/opt/hub/bin/run"]

# In the runfile: App, Java Main Class, Min heap, Max heap, Min new size.
# Loads singleHubMain by default, and we start with modest memory settings.
CMD ["hub", "com.flightstats.hub.app.SingleHubMain", "256m", "512m", "10m"]
