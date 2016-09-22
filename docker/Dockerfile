FROM java:8
# the commented stuff is for if the java8 image doesn't work (it's debian jessie?) and we use ubuntu.
# including timezone adjustment

RUN apt-get update && apt-get -y install ntp curl haproxy # software-properties-common debconf-utils

#RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && apt-get install -y oracle-java8-installer

ADD haproxy.cfg /etc/haproxy/haproxy.cfg

ADD singlehub /opt/hub

ADD run /opt/hub/bin/run

RUN adduser --home /opt/hub --shell /bin/bash --disabled-password --gecos "Hub" hub && chown -R hub:hub /opt/hub && cmod +x /opt/hub/bin/*

EXPOSE 80

USER hub

RUN haproxy -f /etc/haproxy/haproxy.cfg

CMD /opt/hub/bin/run
