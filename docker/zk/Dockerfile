FROM gliderlabs/alpine:3.3
MAINTAINER Flightstats IT Ninjas <it@flightstats.com>

RUN apk-install openjdk7-jre wget bash

RUN mkdir /opt \
    && wget -q -O - http://apache.mirrors.pair.com/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz | tar -xzf - -C /opt \
    && mv /opt/zookeeper-3.4.6 /opt/zookeeper \
    && mkdir -p /tmp/zookeeper

COPY zoo.cfg /opt/zookeeper/conf/zoo.cfg

ARG myid

RUN echo $myid > /tmp/zookeeper/myid

ENV JAVA_HOME /usr/lib/jvm/java-1.7-openjdk

WORKDIR /opt/zookeeper

ENTRYPOINT ["/opt/zookeeper/bin/zkServer.sh"]

CMD ["start-foreground"]

