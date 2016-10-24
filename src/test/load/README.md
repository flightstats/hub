LocustWhale - locust.io in docker
=====================================

![I'm a Mantis!](http://i.imgur.com/NyDBaMn.png "I'm a Mantis!")

Dockerfile: 

```
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

ENTRYPOINT ["/usr/local/bin/locust", "-f"]
```

This creates a docker image with python, locustio, and the scripts in this dir at /locust
which is also the working dir for the container. 

Run it by using something like the following, supplying the filename of the .py test
to run, and the host to run it against :

```
docker run -p 8089:8089 -p 5557:5557 -p 5558:5558 locust:latest single.py --host http://dev.hub-endpoint.org/
```

