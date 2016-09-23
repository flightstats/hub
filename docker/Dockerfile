FROM java:8

RUN apt-get update && apt-get -y install ntp curl haproxy

ADD haproxy.cfg /etc/haproxy/haproxy.cfg

ADD run /opt/hub/bin/run

ADD singlehub /opt/hub

RUN chmod +x /opt/hub/bin/*

EXPOSE 80

ENTRYPOINT /opt/hub/bin/run
