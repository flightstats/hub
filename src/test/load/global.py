# locust.py

import json
import logging
import random
import string
import threading

import time
from flask import request, jsonify
from locust import HttpLocust, TaskSet, task, events, web

import globalConfig

# This test uses the http://locust.io/ framework.
#
# It performs a combination of verification and load testing.
# Each channel (aka user) will perform all of the methods defined by @task(N)
# where N is the relative weighting for frequency.
#
#
# Usage:
#   locust -f read-write-group.py -H http://hub
# or in the background with:
#   nohup locust -f read-write-group.py -H http://hub &
#
# After starting the process, go to http://locust:8089/

logger = logging.getLogger('hub-locust')
logger.setLevel(logging.INFO)
fh = logging.FileHandler('./locust.log')
fh.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
logger.addHandler(fh)

groupCallbacks = {}
groupConfig = {}

print globalConfig.globalConfig


class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        WebsiteTasks.channelNum += 1
        self.number = WebsiteTasks.channelNum
        self.payload = self.payload_generator()
        logger.info("payload size " + str(self.payload.__sizeof__()))
        self.channel = "global_test_" + str(self.number)
        self.count = 0
        self.satellite = globalConfig.globalConfig["satellites"][0]
        payload = {
            "name": self.channel,
            "ttlDays": "3",
            "tags": ["load", "test", "DDT"],
            "owner": "DDT",
            "global": {
                "master": (globalConfig.globalConfig["master"]),
                "satellites": [self.satellite]
            }
        }
        self.client.put("/channel/" + self.channel,
                        data=json.dumps(payload),
                        headers={"Content-Type": "application/json"},
                        name="channel")
        # we want to ensure the channel is created very quickly ...
        self.client.put(self.satellite + "internal/global/satellite/" + self.channel,
                        data=json.dumps(payload),
                        headers={"Content-Type": "application/json"},
                        name="channel")
        self.start_group_callback()
        time.sleep(5)

    def start_group_callback(self):
        # First User - create channel - posts to channel, group callback on Satellite channel
        # Second User - create channel - posts to channel, parallel group callback on Satellite channel
        # Third User - create channel - posts to channel, minute group callback on Satellite channel
        parallel = 1
        batch = "SINGLE"
        heartbeat = False
        if self.number == 2:
            parallel = 2
            heartbeat = True
        if self.number == 3:
            batch = "MINUTE"
        satellite_group_url = self.satellite + "group/locust_" + self.channel

        logger.info("group channel " + self.channel + " parallel:" + str(parallel) + " url " + satellite_group_url)
        self.client.delete(satellite_group_url, name="group")
        groupCallbacks[self.channel] = {
            "data": [],
            "lock": threading.Lock(),
            "parallel": parallel,
            "batch": batch,
            "heartbeat": heartbeat,
            "heartbeats": []
        }
        group = {
            "callbackUrl": "http://" + groupConfig['ip'] + ":8089/callback/" + self.channel,
            "channelUrl": self.satellite + "channel/" + self.channel,
            "parallelCalls": parallel,
            "batch": batch,
            "heartbeat": heartbeat
        }
        self.client.put(satellite_group_url,
                        data=json.dumps(group),
                        headers={"Content-Type": "application/json"},
                        name="group")

    @staticmethod
    def removeHostChannel(href):
        return href.split("/channel/", 1)[1]

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self.client.base_url + "/channel/" + self.channel, 'GET')
        return json.loads(c)

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        postData = json.dumps(payload)
        with self.client.post("/channel/" + self.channel, data=postData,
                              headers={"Content-Type": "application/json"}, catch_response=True,
                              name="post_payload") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = postResponse.json()
        self.count += 1
        href = links['_links']['self']['href']
        self.append_href(href, groupCallbacks)

        if groupCallbacks[self.channel]["heartbeat"]:
            id = href[-30:-14]
            if id not in groupCallbacks[self.channel]["heartbeats"]:
                logger.info("adding heartbeat " + id)
                groupCallbacks[self.channel]["heartbeats"].append(id)

        return href

    def append_href(self, href, obj):
        shortHref = WebsiteTasks.removeHostChannel(href)
        try:
            obj[self.channel]["lock"].acquire()
            obj[self.channel]["data"].append(shortHref)
            logger.debug('wrote %s', shortHref)
        finally:
            obj[self.channel]["lock"].release()

    def read(self, uri):
        with self.client.get(uri, catch_response=True, name="get_payload") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code) + " " + uri)

    @task(1000)
    def write_read(self):
        self.read(self.write())

    @task(100)
    def sequential(self):
        start_time = time.time()
        posted_items = []
        query_items = []
        items = 20
        for x in range(0, items):
            posted_items.append(self.write())
        self.complete_replication()
        initial = (self.client.get(self.time_path("minute"), name="time_minute")).json()
        logger.info("found initial " + ", ".join(initial['_links']['uris']))
        if len(initial['_links']['uris']) < items:
            previous = (self.client.get(initial['_links']['previous']['href'], name="time_minute")).json()
            query_items.extend(previous['_links']['uris'])
        query_items.extend(initial['_links']['uris'])
        logger.info("found query_items " + ", ".join(query_items))
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
        self.complete_replication()
        items = []
        url = self.time_path("minute") + "&trace=true"
        first = (self.client.get(url, name="time_minute")).json()
        second = (self.client.get(first['_links']['previous']['href'] + "&trace=true", name="time_minute")).json()
        items.extend(second['_links']['uris'])
        items.extend(first['_links']['uris'])
        numItems = str(len(items) - 1)
        nextUrl = items[0] + "/next/" + numItems + "?stable=false&trace=true"
        next_json = (self.client.get(nextUrl, name="next")).json()
        next_uris = next_json['_links']['uris']
        if cmp(next_uris, items[1:]) == 0:
            events.request_success.fire(request_type="next", name="compare", response_time=1,
                                        response_length=len(items))
        else:
            logger.info(nextUrl + " next " + ", ".join(items[1:]) + " found " + ", ".join(next_uris))
            logger.info(" first " + json.dumps(first['trace']) + " second " + json.dumps(second['trace']))
            logger.info(" next " + json.dumps(next_json['trace']))
            events.request_failure.fire(request_type="next", name="compare", response_time=1
                                        , exception=-1)

        previousUrl = items[-1] + "/previous/" + numItems + "?stable=false&trace=true"
        previous_json = (self.client.get(previousUrl, name="previous")).json()
        previous_uris = previous_json['_links']['uris']
        if cmp(previous_uris, items[:-1]) == 0:
            events.request_success.fire(request_type="previous", name="compare", response_time=1,
                                        response_length=len(items))
        else:
            logger.info(previousUrl + " previous " + ", ".join(items[:-1]) + " found " + ", ".join(previous_uris))
            logger.info(" first " + json.dumps(first['trace']) + " second " + json.dumps(second['trace']))
            logger.info(" previous " + json.dumps(previous_json['trace']))
            events.request_failure.fire(request_type="previous", name="compare", response_time=1
                                        , exception=-1)

    @staticmethod
    def complete_replication():
        # make sure that global replication should have completed
        time.sleep(10)

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

    def verify_callback(self, obj, name="group"):
        obj[self.channel]["lock"].acquire()
        items = len(obj[self.channel]["data"])
        if items > 500:
            events.request_failure.fire(request_type=name, name="length", response_time=1,
                                        exception=-1)
            logger.info(name + " too many items in " + self.channel + " " + str(items))
        obj[self.channel]["lock"].release()

    @task(10)
    def verify_callback_length(self):
        self.verify_callback(groupCallbacks, "group")
        if groupCallbacks[self.channel]["heartbeat"]:
            heartbeats_ = groupCallbacks[self.channel]["heartbeats"]
            if len(heartbeats_) > 2:
                events.request_failure.fire(request_type="heartbeats", name="length", response_time=1,
                                            exception=-1)
                logger.info(" too many heartbeats in " + self.channel + " " + str(heartbeats_))

    @staticmethod
    def verify_ordered(channel, incoming_uri, obj, name):
        if obj[channel]["data"][0] == incoming_uri:
            (obj[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type=name, name="ordered", response_time=1,
                                        response_length=1)
        else:
            events.request_failure.fire(request_type=name, name="ordered", response_time=1,
                                        exception=-1)
            if incoming_uri in obj[channel]["data"]:
                logger.info(name + " item in the wrong order " + str(incoming_uri) + " data " + \
                            str(obj[channel]["data"]))
                (obj[channel]["data"]).remove(incoming_uri)
            else:
                logger.info("missing item " + str(incoming_uri))

    @staticmethod
    def verify_parallel(channel, incoming_uri):
        if incoming_uri in groupCallbacks[channel]["data"]:
            (groupCallbacks[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type="group", name="parallel", response_time=1,
                                        response_length=1)
        else:
            logger.info("missing parallel item " + str(incoming_uri))
            events.request_failure.fire(request_type="group", name="parallel", response_time=1,
                                        exception=-1)

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        if request.method == "POST":
            incoming_json = request.get_json()
            if incoming_json['type'] == "item":
                for incoming_uri in incoming_json["uris"]:
                    if "_replicated" in incoming_uri:
                        incoming_uri = incoming_uri.replace("_replicated", "")
                    if channel not in groupCallbacks:
                        logger.info("incoming uri before locust tests started " + str(incoming_uri))
                        return "ok"
                    try:
                        shortHref = WebsiteTasks.removeHostChannel(incoming_uri)
                        groupCallbacks[channel]["lock"].acquire()
                        if groupCallbacks[channel]["parallel"] == 1:
                            WebsiteTasks.verify_ordered(channel, shortHref, groupCallbacks, "group")
                        else:
                            WebsiteTasks.verify_parallel(channel, shortHref)
                    finally:
                        groupCallbacks[channel]["lock"].release()
            if incoming_json['type'] == "heartbeat":
                logger.info("heartbeat " + str(incoming_json))
                # make sure the heart beat is before the first data item
                # heartbeat {u'id': u'2015/10/07/01/14', u'type': u'heartbeat', u'name': u'locust_load_test_2', u'uris': []}
                if incoming_json['id'] == groupCallbacks[channel]["heartbeats"][0]:
                    (groupCallbacks[channel]["heartbeats"]).remove(incoming_json['id'])
                    events.request_success.fire(request_type="heartbeats", name="order", response_time=1,
                                                response_length=1)
                else:
                    logger.info("heartbeat order failure. id = " + incoming_json['id'] + " array=" + str(
                        groupCallbacks[channel]["heartbeats"]))
                    events.request_failure.fire(request_type="heartbeats", name="order", response_time=1,
                                                exception=-1)
            return "ok"
        else:
            return jsonify(items=groupCallbacks[channel]["data"])


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 2000
    max_wait = 3000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        groupConfig['host'] = self.host
        groupConfig['ip'] = '10.1.13.245'
        # groupConfig['ip'] = socket.gethostbyname(socket.getfqdn())
        logger.info('groupConfig %s', groupConfig)
        # todo look at using --logfile from https://github.com/locustio/locust/blob/master/locust/main.py#L169
        print groupConfig
