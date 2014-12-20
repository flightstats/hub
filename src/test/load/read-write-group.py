# locust.py

import json
import string
import random
import time
import threading
import socket
import logging

from locust import HttpLocust, TaskSet, task, events, web
from flask import request, jsonify


# Usage:
# locust -f read-write-group.py -H http://localhost:9080
# nohup locust -f read-write-group.py -H http://hub-v2.svc.dev &

logger = logging.getLogger('hub-locust')
logger.setLevel(logging.INFO)
fh = logging.FileHandler('./locust.log')
# fh = logging.FileHandler('/home/ubuntu/locust.log')
fh.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
logger.addHandler(fh)

groupCallbacks = {}
groupConfig = {}


class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        WebsiteTasks.channelNum += 1
        self.number = WebsiteTasks.channelNum
        self.payload = self.payload_generator()
        logger.info("payload size " + str(self.payload.__sizeof__()))
        self.channel = "load_test_" + str(self.number)
        self.count = 0
        payload = {"name": self.channel, "ttlDays": "3"}
        self.client.put("/channel/" + self.channel,
                         data=json.dumps(payload),
                         headers={"Content-Type": "application/json"},
                         name="channel")
        group_name = "/group/locust_" + self.channel
        self.client.delete(group_name, name="group")
        parallel = 1
        if self.number % 2 == 0:
            parallel = 2
        logger.info("channel " + self.channel + " parallel:" + str(parallel))
        groupCallbacks[self.channel] = {
            "data": [],
            "lock": threading.Lock(),
            "parallel": parallel
        }
        group = {
            "callbackUrl": "http://" + groupConfig['ip'] + ":8089/callback/" + self.channel,
            "channelUrl": groupConfig['host'] + "/channel/" + self.channel,
            "parallelCalls": parallel
        }
        self.client.put(group_name,
                        data=json.dumps(group),
                        headers={"Content-Type": "application/json"},
                        name="group")

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        with self.client.post("/channel/" + self.channel, data=json.dumps(payload),
                              headers={"Content-Type": "application/json"}, catch_response=True) as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = postResponse.json()
        self.count += 1
        href = links['_links']['self']['href']
        try:
            groupCallbacks[self.channel]["lock"].acquire()
            groupCallbacks[self.channel]["data"].append(href)
            logger.debug('wrote %s', href)
        finally:
            groupCallbacks[self.channel]["lock"].release()
        return href

    def read(self, uri):
        with self.client.get(uri, catch_response=True, name="get_payload") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code) + " " + uri)

    @task(1000)
    def write_read(self):
        self.read(self.write())

    @task(10)
    def sequential(self):
        start_time = time.time()
        posted_items = []
        query_items = []
        items = 10
        for x in range(0, items):
            posted_items.append(self.write())
        initial = (self.client.get(self.time_path("minute"), name="time_minute")).json()

        if len(initial['_links']['uris']) < items:
            previous = (self.client.get(initial['_links']['previous']['href'], name="time_minute")).json()
            query_items.extend(previous['_links']['uris'])
        query_items.extend(initial['_links']['uris'])
        query_slice = query_items[-items:]
        total_time = int((time.time() - start_time) * 1000)
        if cmp(query_slice, posted_items) == 0:
            events.request_success.fire(request_type="sequential", name="compare", response_time=total_time,
                                        response_length=items)
        else:
            logger.info("expected " + ", ".join(posted_items) + " found " + ", ".join(query_slice))
            events.request_failure.fire(request_type="sequential", name="compare", response_time=total_time
                                        , exception=-1)

    @task(1)
    def day_query(self):
        self.client.get(self.time_path("day"), name="time_day")

    @task(1)
    def hour_query(self):
        self.client.get(self.time_path("hour"), name="time_hour")

    @task(1)
    def hour_query_get_items(self):
        self.next("hour")

    @task(1)
    def minute_query(self):
        self.client.get(self.time_path("minute"), name="time_minute")

    @task(1)
    def minute_query_get_items(self):
        self.next("minute")

    @task(10)
    def next_previous(self):
        items = []
        first = (self.client.get(self.time_path("minute"), name="time_minute")).json()
        second = (self.client.get(first['_links']['previous']['href'], name="time_minute")).json()
        items.extend(second['_links']['uris'])
        items.extend(first['_links']['uris'])

        numItems = str(len(items) - 1)
        url = items[0] + "/next/" + numItems + "?stable=false"
        next = (self.client.get(url, name="next")).json()['_links']['uris']
        if cmp(next, items[1:]) == 0:
            events.request_success.fire(request_type="next", name="compare", response_time=1,
                                        response_length=len(items))
        else:
            logger.info("next " + ", ".join(items[1:]) + " found " + ", ".join(next))
            events.request_failure.fire(request_type="next", name="compare", response_time=1
                                        , exception=-1)

        url = items[-1] + "/previous/" + numItems + "?stable=false"
        previous = (self.client.get(url, name="previous")).json()['_links']['uris']
        if cmp(previous, items[:-1]) == 0:
            events.request_success.fire(request_type="previous", name="compare", response_time=1,
                                        response_length=len(items))
        else:
            logger.info("previous " + ", ".join(items[:-1]) + " found " + ", ".join(previous))
            events.request_failure.fire(request_type="previous", name="compare", response_time=1
                                        , exception=-1)

    @task(10)
    def second_query(self):
        results = self.client.get(self.time_path("second"), name="time_second").json()
        items = 60
        for x in range(0, items):
            results = self.client.get(results['_links']['previous']['href'], name="time_second").json()

    def time_path(self, unit="second"):
        return "/channel/" + self.channel + "/time/" + unit + "?stable=false"

    def next(self, time_unit):
        path = self.time_path(time_unit)
        with self.client.get(path, catch_response=True, name="time_" + time_unit) as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code))
        links = postResponse.json()
        uris = links['_links']['uris']
        if len(uris) > 0:
            for uri in uris:
                self.read(uri)

    def payload_generator(self, chars=string.ascii_uppercase + string.digits):
        size = self.number * self.number * 300
        return ''.join(random.choice(chars) for x in range(size))

    @staticmethod
    def verify_ordered(channel, incoming_uri):
        if groupCallbacks[channel]["data"][0] == incoming_uri:
            (groupCallbacks[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type="group", name="ordered", response_time=1,
                                        response_length=1)
        else:
            events.request_failure.fire(request_type="group", name="ordered", response_time=1,
                                        exception=-1)
            if incoming_uri in groupCallbacks[channel]["data"]:
                (groupCallbacks[channel]["data"]).remove(incoming_uri)
                logger.info("item in the wrong order " + str(incoming_uri) + " data " + \
                      str(groupCallbacks[channel]["data"]))
            else:
                logger.info("missing item " + str(incoming_uri))

    @staticmethod
    def verify_parallel(channel, incoming_uri):
        if incoming_uri in groupCallbacks[channel]["data"]:
            (groupCallbacks[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type="group", name="parallel", response_time=1,
                                        response_length=1)
        else:
            events.request_failure.fire(request_type="group", name="parallel", response_time=1,
                                        exception=-1)

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        if request.method == 'POST':
            incoming_json = request.get_json()
            incoming_uri = incoming_json['uris'][0]
            if channel not in groupCallbacks:
                logger.info("incoming uri before locust tests started " + str(incoming_uri))
                return "ok"
            try:
                groupCallbacks[channel]["lock"].acquire()
                if groupCallbacks[channel]["parallel"] == 1:
                    WebsiteTasks.verify_ordered(channel, incoming_uri)
                else:
                    WebsiteTasks.verify_parallel(channel, incoming_uri)
            finally:
                groupCallbacks[channel]["lock"].release()
            return "ok"
        else:
            return jsonify(items=groupCallbacks[channel]["data"])


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 400
    max_wait = 900

    def __init__(self):
        super(WebsiteUser, self).__init__()
        groupConfig['host'] = self.host
        groupConfig['ip'] = socket.gethostbyname(socket.getfqdn())
        logger.info('groupConfig %s', groupConfig)
        print groupConfig

