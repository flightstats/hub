LocustWhale - locust.io in docker
=====================================

![Yeah I know, he's a mantis](http://i.imgur.com/NyDBaMn.png "Locustmantis has too many Ts")

Dockerfile: 

```
FROM alpine:3.2

RUN apk -U add ca-certificates python python-dev py-pip build-base && \
    pip install locustio pyzmq websocket httplib2 && \
    apk del python-dev && \
    rm -r /var/cache/apk/* && \
    mkdir /locust

WORKDIR /locust


ADD . /locust

EXPOSE 8089 5557 5558
```

This creates a docker image with python, locustio, and the scripts in this dir at /locust.

Run it by using something like the following:

```
docker run --name locust-verifier locust:latest /usr/bin/locust -f single.py --host http://hub-endpoint:8080/
```

