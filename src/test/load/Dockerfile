FROM ubuntu:xenial

RUN apt-get update && \
    apt-get install -y build-essential libreadline-gplv2-dev libncursesw5-dev libssl-dev libsqlite3-dev tk-dev libgdbm-dev libc6-dev libbz2-dev \
    python2.7 python-dev python-pip libevent-dev

RUN pip install locustio pyzmq websocket-client httplib2 && \
    mkdir /locust

WORKDIR /locust

#RUN test -f requirements.txt && pip install -r requirements.txt

ADD . /locust

EXPOSE 8089 5557 5558
