import httplib2
import json
import logging
import random
import socket
import string
import thread
import threading
import time
import websocket
from datetime import datetime, timedelta
from flask import request, jsonify
from locust import events
from websocket import WebSocketException

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

webhooks = {}
webhookLocks = {}
webhookConfig = {}
websockets = {}
websocketLocks = {}
skip_verify_ordered = False


# BasicTasks is meant to be used as a common class for running tests
# The using class must define the 'host' value

def key_from_time(dt, unit="second"):
    key_options = {"second": dt.strftime("%Y/%m/%d/%H/%M/%S"),
                   "minute": dt.strftime("%Y/%m/%d/%H/%M"),
                   "hour": dt.strftime("%Y/%m/%d/%H"),
                   "day": dt.strftime("%Y/%m/%d"),
                   "month": dt.strftime("%Y/%m"),
                   "year": dt.strftime("%Y"),
                   }
    return key_options[unit]

def webhook_name(channel):
    return "/webhook/locust_" + str(channel)

class HubTasks:
    host = None
    channelNum = 0

    def __init__(self, user, client):
        self.user = user
        self.client = client

    def start(self):
        webhookConfig['host'] = HubTasks.host
        webhookConfig['ip'] = socket.gethostbyname(socket.getfqdn())
        logger.info('webhookConfig %s', webhookConfig)

        HubTasks.channelNum += 1
        self.number = HubTasks.channelNum
        self.payload = self.payload_generator()
        logger.info("payload size " + str(self.payload.__sizeof__()))
        logger.info("user name " + str(self.user.name()))
        self.channel = self.user.name() + str(self.number)
        self.count = 0
        payload = {"name": self.channel, "ttlDays": "3", "tags": ["load", "test", "DDT"], "owner": "DDT"}
        logger.info('webhook: ' + json.dumps(payload))
        self.user.start_channel(payload, self)
        self.client.put("/channel/" + self.channel,
                        data=json.dumps(payload),
                        headers={"Content-Type": "application/json"},
                        name="channel")

        if self.user.has_webhook():
            self.start_webhook()
        if self.user.has_websocket():
            self.start_websocket()
        skip_verify_ordered = self.user.skip_verify_ordered()
        if skip_verify_ordered:
            logger.warn("skipping verify_ordered")

        time.sleep(5)

    def webhook_config(self):
        config = {
            "number": self.number,
            "channel": self.channel,
            "webhook_channel": self.channel,
            "parallel": 1,
            "batch": "SINGLE",
            "heartbeat": False,
            "client": self.client,
            "host": self.host
        }
        self.user.start_webhook(config)
        return config

    def upsert_webhook(self, overrides={}):
        config = self.webhook_config()
        wh_config = {
            "callbackUrl": "http://" + webhookConfig['ip'] + ":8089/callback/" + self.channel,
            "channelUrl": webhookConfig['host'] + "/channel/" + config['webhook_channel'],
            "parallelCalls": config['parallel'],
            "batch": config['batch'],
            "heartbeat": config['heartbeat']
        }
        wh_config.update(overrides)
        uri = webhook_name(self.channel)
        logger.info('PUT ' + uri + ' ' + json.dumps(wh_config))
        self.client.put(uri,
                        data=json.dumps(wh_config),
                        headers={"Content-Type": "application/json"},
                        name="webhook")

    def start_webhook(self):
        logger.info('starting webhook')
        config = self.webhook_config()
        webhook = webhook_name(self.channel)
        self.client.delete(webhook, name="webhook")
        webhooks[self.channel] = {
            "start": datetime.now(),
            "data": [],
            "parallel": config['parallel'],
            "batch": config['batch'],
            "heartbeat": config['heartbeat'],
            "heartbeats": [],
            "lastHeartbeat": '',
            "unknown": [],
        }
        webhookLocks[self.channel] = threading.Lock()
        logger.info('webhook store for "' + self.channel + '": ' + json.dumps(webhooks[self.channel], default=str))
        self.upsert_webhook()

    def get_webhook_config(self):
        json = (self.client.get(webhook_name(self.channel), name="webhook_config")).json()
        return json

    def get_webhook_last_completed(self):
        config = self.get_webhook_config()
        return config["lastCompleted"]

    def get_channel_latest_date(self):
        config = self.get_webhook_config()
        channel_latest_url = config["channelLatest"]

    def channel_url_from_time(self, time, unit="second"):
        return self.host + "/channel/" + self.channel + "/" + key_from_time(time, unit)

        # update webhook cursor to now "current=True" or 1 day in the past

    def update_webhook_cursor(self, current=True):
        if current:
            url = self.channel_url_from_time(datetime.now(), "second")
        else:
            yesterday = datetime.now() - timedelta(days=1)
            url = self.channel_url_from_time(yesterday, "second")
        logger.info("updating webhook with url:  " + url)
        data = {
            "item": url
        }
        self.client.put(webhook_name(self.channel) + "/updateCursor", data=json.dumps(data),
                        headers={"Content-Type": "application/json"})

    def perform_cursor_update(self, update_to_yesterday, update_to_now, name="upsertCursor"):
        # get current latest completed
        old_latest = self.get_webhook_last_completed()
        # update cursor
        update_to_yesterday()
        # wait a bit
        time.sleep(2)
        new_latest = self.get_webhook_last_completed()

        # verify that new latest < old latest
        it_works = new_latest < old_latest

        update_to_now()
        time.sleep(1)

        if it_works:
            events.request_success.fire(request_type="webhook", name=name, response_time=1,
                                        response_length=1)
        else:
            events.request_failure.fire(request_type="webhook", name=name, response_time=1,
                                        exception=-1)

    def verify_cursor_update(self):
        update_to_yesterday = lambda: self.update_webhook_cursor(False)
        update_to_now = lambda: self.update_webhook_cursor(True)
        self.perform_cursor_update(update_to_yesterday, update_to_now, "updateCursor")

    def verify_cursor_update_via_upsert(self):
        update_to_yesterday = lambda: self.upsert_webhook(
            overrides={"startItem": self.channel_url_from_time(datetime.now() - timedelta(days=1))})
        update_to_now = lambda: self.upsert_webhook(overrides={"startItem": self.channel_url_from_time(datetime.now())})
        self.perform_cursor_update(update_to_yesterday, update_to_now, "upsertWebhook")

    def start_websocket(self):
        self._http = httplib2.Http()
        meta = self._load_metadata()
        self.ws_uri = meta['_links']['ws']['href']
        logger.info(self.ws_uri)
        ws = websocket.WebSocketApp(self.ws_uri,
                                    on_open=self.on_open,
                                    on_message=self.on_message,
                                    on_close=self.on_close,
                                    on_error=self.on_error)
        channel = self.channel

        def restart_websocket_server_on_close():
            while True:
                try:
                    websocketLocks[channel] = threading.Lock()
                    websockets[channel] = {
                        "data": [],
                        "unknown": [],
                        "open": True,
                        "start": datetime.now()
                    }
                    logger.info('websocket store for "' + channel + '": ' + json.dumps(websockets[channel], default=str))
                    ws.run_forever()
                except WebSocketException:
                    logger.exception('WebSocket client was meant to run forever but was stopped.')
                    pass

        thread.start_new_thread(restart_websocket_server_on_close, ())

    @staticmethod
    def get_short_path(url):
        url_components = url.split("/channel/", 1)
        if not len(url_components) == 2:
            raise ValueError('Unable to determine short path for "' + url + '"')
        return url_components[1]

    def on_open(self, ws):
        logger.info('websocket | ' + self.channel + ' | opened')
        websockets[self.channel]['open'] = True

    def on_close(self, ws):
        logger.info('websocket | ' + self.channel + ' | closed')
        websockets[self.channel]['open'] = False

    def on_message(self, ws, message):
        logger.debug('websocket | ' + self.channel + ' | message: ' + message)
        content_key = HubTasks.get_short_path(message)
        timestamp = get_item_timestamp(content_key)
        if timestamp < websockets[self.channel]['start']:
            logger.info('item before start time: ' + content_key)
            return
        HubTasks.verify_ordered(self.channel, content_key, websockets, "websocket")

    def on_error(self, ws, error):
        logger.error('websocket | ' + self.channel + ' | error: ' + str(error))
        websockets[self.channel]['open'] = False

    def _load_metadata(self):
        logger.info("Fetching channel metadata...")
        r, c = self._http.request(self.client.base_url + "/channel/" + self.channel, 'GET')
        return json.loads(c)

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        with self.client.post(self.get_channel_url(),
                              data=(json.dumps(payload)),
                              headers={"Content-Type": "application/json"},
                              catch_response=True,
                              name="post_payload") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        return self.parse_write(postResponse)

    def get_channel_url(self):
        return self.user.channel_post_url(self.channel)

    def parse_write(self, postResponse):
        try:
            links = postResponse.json()
        except ValueError:
            logger.warning('invalid response: ' + postResponse.status_code + ' ' + postResponse.text)
            raise

        self.count += 1
        href = links['_links']['self']['href']
        if self.user.has_webhook():
            self.append_href(href, 'webhook')
            if webhooks[self.channel]["heartbeat"]:
                if webhooks[self.channel]["batch"] == "MINUTE":
                    id = href[-30:-14]
                else:
                    id = href[-30:-11]
                if id not in webhooks[self.channel]["heartbeats"]:
                    logger.info("adding heartbeat " + id)
                    webhooks[self.channel]["heartbeats"].append(id)
        if self.user.has_websocket():
            if websockets[self.channel]["open"]:
                self.append_href(href, 'websocket')
        return href

    def append_href(self, href, store_name):
        content_key = HubTasks.get_short_path(href)
        store = get_store_by_name(store_name)
        lock = get_lock_by_store_name(store_name, self.channel)
        try:
            lock.acquire()
            store[self.channel]["data"].append(content_key)
        finally:
            lock.release()

    def read(self, uri, verify=False):
        checkCount = self.count - 1
        with self.client.get(uri, catch_response=True, name="get_payload") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code) + " " + uri)
            elif verify:
                if str(checkCount) not in postResponse.content:
                    logger.info("wrong response " + uri + " " + postResponse.content)
                    postResponse.failure("Got wrong checkCount on get: " + str(postResponse.status_code) + " " + uri)

    def change_parallel(self, channel):
        group = {
            "callbackUrl": "http://" + webhookConfig['ip'] + ":8089/callback/" + channel,
            "channelUrl": webhookConfig['host'] + "/channel/" + channel,
            "parallelCalls": random.randint(1, 5)
        }
        self.client.put("/group/locust_" + channel,
                        data=json.dumps(group),
                        headers={"Content-Type": "application/json"},
                        name="webhook")

    def write_read(self):
        self.read(self.write(), True)

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

    def latest(self):
        self.client.get('/channel/' + self.channel + '/latest', name="latest")

    def earliest(self):
        self.client.get('/channel/' + self.channel + '/earliest', name="earliest")

    def day_query(self):
        self.client.get(self.time_path("day"), name="time_day")

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
        url = self.time_path("minute")
        first = (self.client.get(url, name="time_minute")).json()
        second = (self.client.get(first['_links']['previous']['href'], name="time_minute")).json()
        items.extend(second['_links']['uris'])
        items.extend(first['_links']['uris'])
        numItems = str(len(items) - 1)
        nextUrl = items[0] + "/next/" + numItems + "?stable=false"
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

        previousUrl = items[-1] + "/previous/" + numItems + "?stable=false"
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
        return "/channel/" + self.channel + self.user.time_path(unit) + "?stable=false"

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

    def next_10(self):
        utcnow = datetime.utcnow()
        self.doNext(utcnow + timedelta(minutes=-1))
        self.doNext(utcnow + timedelta(hours=-1))
        self.doNext(utcnow + timedelta(days=-1))

    def doNext(self, time):
        path = "/channel/" + self.channel + time.strftime("/%Y/%m/%d/%H/%M/%S/000") + "/A/next/10"
        with self.client.get(path, catch_response=True, name="next") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on next: " + str(postResponse.status_code))

    def payload_generator(self, chars=string.ascii_uppercase + string.digits):
        size = self.number * self.number * 300
        return ''.join(random.choice(chars) for x in range(size))

    def verify_callback(self, obj, lock_store, name="webhook", count=2000):
        lock_store[self.channel].acquire()
        items = len(obj[self.channel]["data"])
        if items > count:
            events.request_failure.fire(request_type=name, name="length", response_time=1, exception=-1)
            logger.info(name + " too many items in " + self.channel + " " + str(items))
        else:
            events.request_success.fire(request_type=name, name="length", response_time=1, response_length=1)
        lock_store[self.channel].release()

    def verify_callback_length(self, count=2000):
        self.verify_callback(webhooks, webhookLocks, "webhook", count)
        if self.user.has_websocket():
            if websockets[self.channel]["open"]:
                self.verify_callback(websockets, websocketLocks, "websocket")
        if webhooks[self.channel]["heartbeat"]:
            heartbeats_ = webhooks[self.channel]["heartbeats"]
            if len(heartbeats_) > 2:
                events.request_failure.fire(request_type="heartbeats", name="length", response_time=1,
                                            exception=-1)
                logger.info(" too many heartbeats in " + self.channel + " " + str(heartbeats_))

    @staticmethod
    def verify_ordered(channel, incoming_uri, obj, name):
        if skip_verify_ordered:
            logger.debug("skipping verify_order")
            return

        if obj[channel]["data"][0] == incoming_uri:
            (obj[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type=name, name="ordered", response_time=1, response_length=1)
        else:
            logger.info(name + ' | expected ' + obj[channel]['data'][0] + ' to be ' + incoming_uri)
            if incoming_uri in obj[channel]["data"]:
                events.request_failure.fire(request_type=name, name="ordered", response_time=1, exception='item in wrong order')
                logger.info(name + ' | ordered | item in wrong order: ' + incoming_uri + ' | found at ' + str(obj[channel]['data'].index(incoming_uri)))
                (obj[channel]["data"]).remove(incoming_uri)
            else:
                obj[channel]["unknown"].append(str(incoming_uri))
                events.request_failure.fire(request_type=name, name="ordered", response_time=1, exception='item unknown')
                logger.info(name + ' | ordered | item unknown: ' + incoming_uri)

    @staticmethod
    def verify_parallel(channel, incoming_uri):
        if incoming_uri in webhooks[channel]["data"]:
            (webhooks[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type="webhook", name="parallel", response_time=1, response_length=1)
        else:
            logger.info('webhook | parallel | item unknown: ' + incoming_uri)
            webhooks[channel]["unknown"].append(str(incoming_uri))
            events.request_failure.fire(request_type="webhook", name="parallel", response_time=1, exception='item unknown')

    @staticmethod
    def get_channels():
        # todo - add errors to output
        return jsonify(items=webhooks)

    @staticmethod
    def callback(channel):
        if request.method == 'POST':
            incoming_json = request.get_json()
            if incoming_json['type'] == "item" or incoming_json['type'] == "items":
                HubTasks.item(channel, incoming_json)
            if incoming_json['type'] == "heartbeat":
                HubTasks.heartbeat(channel, incoming_json)
            return "ok"
        else:
            return jsonify(items=webhooks[channel]["data"])

    @staticmethod
    def item(channel, incoming_json):
        # todo handle case of first partial minute
        for incoming_uri in incoming_json['uris']:
            if "_replicated" in incoming_uri:
                incoming_uri = incoming_uri.replace("_replicated", "")
            if channel not in webhooks:
                logger.info("incoming uri before locust tests started " + str(incoming_uri))
                return
            try:
                content_key = HubTasks.get_short_path(incoming_uri)
                timestamp = get_item_timestamp(content_key)
                if timestamp < webhooks[channel]['start']:
                    logger.info('item before start time: ' + content_key)
                    return

                webhookLocks[channel].acquire()
                if webhooks[channel]["parallel"] == 1:
                    HubTasks.verify_ordered(channel, content_key, webhooks, "webhook")
                else:
                    HubTasks.verify_parallel(channel, content_key)
            finally:
                if not webhookLocks[channel].locked():
                    logger.warning('no lock to release: webhookLocks[' + channel + ']')
                else:
                    webhookLocks[channel].release()

    @staticmethod
    def heartbeat(channel, incoming_json):
        if not webhooks[channel]["heartbeat"]:
            return
        heartbeats_ = webhooks[channel]["heartbeats"]
        id_ = incoming_json['id']
        if id_ == heartbeats_[0]:
            heartbeats_.remove(id_)
            events.request_success.fire(request_type="heartbeats", name="order", response_time=1, response_length=1)
        elif id_ != webhooks[channel]["lastHeartbeat"]:
            if id_ in heartbeats_:
                heartbeats_.remove(id_)
            events.request_success.fire(request_type="heartbeats", name="order", response_time=1, response_length=1)
        else:
            logger.info("heartbeat order failure. id = " + id_ + " array=" + str(heartbeats_))
            events.request_failure.fire(request_type="heartbeats", name="order", response_time=1, exception=-1)
        webhooks[channel]["lastHeartbeat"] = id_

    @staticmethod
    def get_store(name):
        return json.dumps(get_store_by_name(name), indent=2, default=str)


def get_store_by_name(name):
    if name == 'websocket':
        return websockets
    elif name == 'webhook':
        return webhooks
    else:
        raise ValueError(name)


def get_lock_by_store_name(store_name, channel):
    if store_name == 'websocket':
        return websocketLocks[channel]
    elif store_name == 'webhook':
        return webhookLocks[channel]
    else:
        raise ValueError(store_name)


def get_item_timestamp(content_key):
    components = content_key.split('/')
    year = int(components[1])
    month = int(components[2])
    day = int(components[3])
    hour = int(components[4])
    minute = int(components[5])
    second = int(components[6])
    millisecond = int(components[7])
    return datetime(year, month, day, hour, minute, second, millisecond * 1000)
