import json
import logging
import random
import socket
import string
import threading

import httplib2
import thread
import time
import websocket
from flask import request, jsonify
from locust import events

logger = logging.getLogger('hub-locust')
logger.setLevel(logging.INFO)
fh = logging.FileHandler('./locust.log')
fh.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
logger.addHandler(fh)

groupCallbacks = {}
groupCallbackLocks = {}
groupConfig = {}
websockets = {}


# BasicTasks is meant to be used as a common class for running tests
# The using class must define the 'user' and 'host' objects


class HubTasks:
    host = None

    channelNum = 0

    def __init__(self, user):
        self.user = user
        self.client = user.client

    def on_start(self):
        groupConfig['host'] = HubTasks.host
        groupConfig['ip'] = socket.gethostbyname(socket.getfqdn())
        logger.info('groupConfig %s', groupConfig)
        print groupConfig

        HubTasks.channelNum += 1
        self.number = HubTasks.channelNum
        self.payload = self.payload_generator()
        logger.info("payload size " + str(self.payload.__sizeof__()))
        logger.info("user name " + str(self.user.name()))
        self.channel = self.user.name() + str(self.number)
        self.count = 0
        payload = {"name": self.channel, "ttlDays": "3", "tags": ["load", "test", "DDT"], "owner": "DDT"}
        self.client.put("/channel/" + self.channel,
                        data=json.dumps(payload),
                        headers={"Content-Type": "application/json"},
                        name="channel")

    def start_group_callback(self):
        # First User - create channel - posts to channel, group callback on channel
        # Second User - create channel - posts to channel, parallel group callback on channel
        # Third User - create channel - posts to channel, replicate channel, group callback on replicated channel
        group_channel = self.channel
        parallel = 1
        batch = "SINGLE"
        heartbeat = False
        if self.number == 2:
            parallel = 2
            heartbeat = True
        if self.number == 3:
            group_channel = self.channel + "_replicated"
            self.client.put("/channel/" + group_channel,
                            data=json.dumps({"name": group_channel, "ttlDays": "3",
                                             "replicationSource": groupConfig['host'] + "/channel/" + self.channel}),
                            headers={"Content-Type": "application/json"},
                            name="replication")
        group_name = "/group/locust_" + group_channel
        self.client.delete(group_name, name="group")
        logger.info("group channel " + group_channel + " parallel:" + str(parallel))
        groupCallbacks[self.channel] = {
            "data": [],
            "parallel": parallel,
            "batch": batch,
            "heartbeat": heartbeat,
            "heartbeats": []
        }
        groupCallbackLocks[self.channel] = {
            "lock": threading.Lock(),
        }
        group = {
            "callbackUrl": "http://" + groupConfig['ip'] + ":8089/callback/" + self.channel,
            "channelUrl": groupConfig['host'] + "/channel/" + group_channel,
            "parallelCalls": parallel,
            "batch": batch,
            "heartbeat": heartbeat
        }
        self.client.put(group_name,
                        data=json.dumps(group),
                        headers={"Content-Type": "application/json"},
                        name="group")

    def start_websocket(self):
        websockets[self.channel] = {
            "data": [],
            "open": True
        }
        self._http = httplib2.Http()
        meta = self._load_metadata()
        self.ws_uri = meta['_links']['ws']['href']
        print self.ws_uri
        ws = websocket.WebSocketApp(self.ws_uri,
                                    on_message=self.on_message,
                                    on_close=self.on_close,
                                    on_error=self.on_error)
        thread.start_new_thread(ws.run_forever, ())

    @staticmethod
    def removeHostChannel(href):
        return href.split("/channel/", 1)[1]

    def on_message(self, ws, message):
        logger.debug("ws %s", message)
        shortHref = HubTasks.removeHostChannel(message)
        HubTasks.verify_ordered(self.channel, shortHref, websockets, "websocket")

    def on_close(self, ws):
        logger.info("closing ws %s", self.channel)
        websockets[self.channel]["open"] = False

    def on_error(self, ws, error):
        logger.info("error ws %s", self.channel)
        websockets[self.channel]["open"] = False

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self.client.base_url + "/channel/" + self.channel, 'GET')
        return json.loads(c)

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        postData = json.dumps(payload)
        if random.random() >= 0.99:
            postData = None

        with self.client.post("/channel/" + self.channel, data=postData,
                              headers={"Content-Type": "application/json"}, catch_response=True,
                              name="post_payload") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = postResponse.json()
        self.count += 1
        href = links['_links']['self']['href']
        self.append_href(href, groupCallbacks)
        if websockets[self.channel]["open"]:
            self.append_href(href, websockets)
        if groupCallbacks[self.channel]["heartbeat"]:
            id = href[-30:-14]
            if id not in groupCallbacks[self.channel]["heartbeats"]:
                logger.info("adding heartbeat " + id)
                groupCallbacks[self.channel]["heartbeats"].append(id)

        return href

    def append_href(self, href, obj):
        shortHref = HubTasks.removeHostChannel(href)
        try:
            groupCallbackLocks[self.channel]["lock"].acquire()
            obj[self.channel]["data"].append(shortHref)
            logger.debug('wrote %s', shortHref)
        finally:
            groupCallbackLocks[self.channel]["lock"].release()

    def read(self, uri):
        with self.client.get(uri, catch_response=True, name="get_payload") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code) + " " + uri)

    def change_parallel(self):
        if self.number % 3 == 2:
            group = {
                "callbackUrl": "http://" + groupConfig['ip'] + ":8089/callback/" + self.channel,
                "channelUrl": groupConfig['host'] + "/channel/" + self.channel,
                "parallelCalls": random.randint(1, 5)
            }
            self.client.put("/group/locust_" + self.channel,
                            data=json.dumps(group),
                            headers={"Content-Type": "application/json"},
                            name="group")

    def write_read(self):
        self.read(self.write())

    def sequential(self):
        start_time = time.time()
        posted_items = []
        query_items = []
        items = 20
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

    def hour_query(self):
        self.client.get(self.time_path("hour"), name="time_hour")

    def hour_query_get_items(self):
        self.next("hour")

    def minute_query(self):
        self.client.get(self.time_path("minute"), name="time_minute")

    def minute_query_get_items(self):
        self.next("minute")

    def next_previous(self):
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
        groupCallbackLocks[self.channel]["lock"].acquire()
        items = len(obj[self.channel]["data"])
        if items > 500:
            events.request_failure.fire(request_type=name, name="length", response_time=1,
                                        exception=-1)
            logger.info(name + " too many items in " + self.channel + " " + str(items))
        groupCallbackLocks[self.channel]["lock"].release()

    def verify_callback_length(self):
        self.verify_callback(groupCallbacks, "group")
        if websockets[self.channel]["open"]:
            self.verify_callback(websockets, "websocket")
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

    @staticmethod
    def get_channels():
        return jsonify(items=groupCallbacks)

    @staticmethod
    def callback(channel):
        if request.method == 'POST':
            incoming_json = request.get_json()
            if incoming_json['type'] == "item":
                HubTasks.item(channel, incoming_json)
            if incoming_json['type'] == "heartbeat":
                HubTasks.heartbeat(channel, incoming_json)
            return "ok"
        else:
            return jsonify(items=groupCallbacks[channel]["data"])

    @staticmethod
    def item(channel, incoming_json):
        for incoming_uri in incoming_json['uris']:
            if "_replicated" in incoming_uri:
                incoming_uri = incoming_uri.replace("_replicated", "")
            if channel not in groupCallbacks:
                logger.info("incoming uri before locust tests started " + str(incoming_uri))
                return
            try:
                shortHref = HubTasks.removeHostChannel(incoming_uri)
                groupCallbackLocks[channel]["lock"].acquire()
                if groupCallbacks[channel]["parallel"] == 1:
                    HubTasks.verify_ordered(channel, shortHref, groupCallbacks, "group")
                else:
                    HubTasks.verify_parallel(channel, shortHref)
            finally:
                groupCallbackLocks[channel]["lock"].release()

    @staticmethod
    def heartbeat(channel, incoming_json):
        logger.info("heartbeat " + str(incoming_json))
        heartbeats_ = groupCallbacks[channel]["heartbeats"]
        if incoming_json['id'] == heartbeats_[0]:
            heartbeats_.remove(incoming_json['id'])
            events.request_success.fire(request_type="heartbeats", name="order", response_time=1, response_length=1)
        elif incoming_json['id'] != groupCallbacks[channel]["lastHeartbeat"]:
            logger.info("heartbeat order question. id = " + incoming_json['id'] + " array=" + str(heartbeats_))
            events.request_success.fire(request_type="heartbeats", name="order", response_time=1, response_length=1)
        else:
            logger.info("heartbeat order failure. id = " + incoming_json['id'] + " array=" + str(heartbeats_))
            events.request_failure.fire(request_type="heartbeats", name="order", response_time=1, exception=-1)
        groupCallbacks[channel]["lastHeartbeat"] = incoming_json['id']
