FROM java:8-alpine

RUN apk update && apk add bash openntpd curl haproxy

ADD haproxy.cfg /etc/haproxy/haproxy.cfg

ADD run /opt/hub/bin/run

ADD singlehub /opt/hub

RUN chmod +x /opt/hub/bin/*

EXPOSE 80

ENTRYPOINT /bin/bash /opt/hub/bin/run
