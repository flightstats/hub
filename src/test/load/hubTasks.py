import json
import logging
import random
import socket
import string
import thread
import threading
import time
from urlparse import urlparse

import requests
from websocket import WebSocketApp
from datetime import datetime, timedelta
from flask import request, jsonify
from locust import events
from websocket import WebSocketException

logger = logging.getLogger(__name__)

webhooks = {}
webhookConfig = {}
websockets = {}
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
            logger.debug("skipping verify_ordered")

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
        logger.debug('PUT ' + uri + ' ' + json.dumps(wh_config))
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
            "lock": threading.Lock()
        }
        logger.info('webhook store for "' + self.channel + '": ' + json.dumps(webhooks[self.channel], default=str))
        self.upsert_webhook()

    def get_webhook_config(self):
        response = self.client.get(webhook_name(self.channel), name="webhook_config")
        return get_response_as_json(response)

    def get_webhook_last_completed(self):
        config = self.get_webhook_config()
        return config["lastCompleted"]

    def get_channel_latest_date(self):
        config = self.get_webhook_config()
        channel_latest_url = config["channelLatest"]

    def channel_url_from_time(self, time, unit="second"):
        return self.host + "/channel/" + self.channel + "/" + key_from_time(time, unit)

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
            events.request_success.fire(request_type="webhook", name=name, response_time=1, response_length=1)
        else:
            events.request_failure.fire(request_type="webhook", name=name, response_time=1, exception='incorrect cursor position')
            logger.error(name + ' | webhook | incorrect cursor position: ' + new_latest + ' > ' + old_latest)

    def verify_cursor_update(self):
        update_to_yesterday = lambda: self.update_webhook_cursor(False)
        update_to_now = lambda: self.update_webhook_cursor(True)
        self.perform_cursor_update(update_to_yesterday, update_to_now, "updateCursor")

    def verify_cursor_update_via_upsert(self):
        update_to_yesterday = lambda: self.upsert_webhook(
            overrides={"startItem": self.channel_url_from_time(datetime.now() - timedelta(days=1))})
        update_to_now = lambda: self.upsert_webhook(overrides={"startItem": self.channel_url_from_time(datetime.now())})
        self.perform_cursor_update(update_to_yesterday, update_to_now, "upsertWebhook")

    def get_websocket_url(self):
        hub_channel_resource = self.client.base_url + '/channel'
        if self.channel in websockets and len(websockets[self.channel]['data']) > 0:
            next_expected_item = hub_channel_resource + '/' + websockets[self.channel]['data'][0]
            start_item = get_previous_url(next_expected_item)
            return convert_http_to_ws_url(start_item)
        else:
            channel_url = hub_channel_resource + '/' + self.channel
            return convert_http_to_ws_url(channel_url)

    def open_websocket(self):
        url = self.get_websocket_url()
        headers = None
        logger.info('opening websocket: ' + url)
        return WebSocketApp(url, headers, self.on_open, self.on_message, self.on_error, self.on_close)

    def restart_websocket_server_on_close(self):
        while True:
            try:
                ws = self.open_websocket()
                ws.run_forever()
            except WebSocketException as e:
                logger.exception(e)
                pass

    def start_websocket(self):
        thread.start_new_thread(self.restart_websocket_server_on_close, ())

    @staticmethod
    def get_short_path(url):
        url_components = url.split("/channel/", 1)
        if not len(url_components) == 2:
            raise ValueError('Unable to determine short path for "' + url + '"')
        return url_components[1]

    def on_open(self, ws):
        logger.info('websocket | ' + self.channel + ' | opened')
        if self.channel not in websockets:
            start_time = datetime.now()
            logger.info('creating new websocket store for ' + self.channel + ' at ' + start_time.isoformat())
            websockets[self.channel] = {
                "data": [],
                "unknown": [],
                "open": True,
                "start": start_time,
                "lock": threading.Lock()
            }
        else:
            websockets[self.channel]['open'] = True

    def on_close(self, ws):
        logger.info('websocket | ' + self.channel + ' | closed')
        if self.channel in websockets:
            websockets[self.channel]['open'] = False

    def on_message(self, ws, message):
        logger.debug('websocket | ' + self.channel + ' | message: ' + message)
        short_href = HubTasks.get_short_path(message)
        timestamp = get_item_timestamp(short_href)
        if timestamp < websockets[self.channel]['start']:
            logger.info('item before start time: ' + short_href)
            return
        HubTasks.verify_ordered(self.channel, short_href, websockets, "websocket")

    def on_error(self, ws, error):
        logger.error('websocket | ' + self.channel + ' | error: ' + str(error))
        if self.channel in websockets:
            websockets[self.channel]['open'] = False

    def write(self):
        url = self.get_channel_url()
        headers = {"Content-Type": "application/json"}
        payload = json.dumps({"name": self.payload, "count": self.count})
        response = self.http_post(url, headers=headers, data=payload, catch_response=True, name="payload")

        if response.status_code != 201:
            response.failure('{} {}'.format(response.status_code, url))

        return self.parse_write(response)

    def get_channel_url(self):
        return self.user.channel_post_url(self.channel)

    def parse_write(self, response):
        links = get_response_as_json(response)

        self.count += 1
        href = links['_links']['self']['href']
        logger.debug('item POSTed: ' + href)
        if self.user.has_webhook():
            self.append_href(href, 'webhooks')
            if webhooks[self.channel]["heartbeat"]:
                if webhooks[self.channel]["batch"] == "MINUTE":
                    id = href[-30:-14]
                else:
                    id = href[-30:-11]
                if id not in webhooks[self.channel]["heartbeats"]:
                    logger.debug("adding heartbeat " + id)
                    webhooks[self.channel]["heartbeats"].append(id)
        if self.user.has_websocket():
            if websockets[self.channel]["open"]:
                self.append_href(href, 'websockets')
        return href

    def append_href(self, href, store_name='webhooks'):
        store = get_store_by_name(store_name)
        short_href = HubTasks.get_short_path(href)
        try:
            store[self.channel]['lock'].acquire()
            store[self.channel]["data"].append(short_href)
        finally:
            store[self.channel]['lock'].release()

    def read(self, url, verify=False):
        response = self.http_get(url, catch_response=True, name="payload")

        if response.status_code != 200:
            response.failure('{} {}'.format(response.status_code, url))

        if verify:
            content = get_response_as_json(response)
            actual = content['count']
            expected = self.count - 1
            if actual != expected:
                message = 'actual {}, expected {} - {}'.format(expected, actual, url)
                logger.error(message)
                response.failure(message)

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
        initial_response = self.client.get(self.time_path("minute"), name="time_minute")
        initial_json = get_response_as_json(initial_response)

        if len(initial_json['_links']['uris']) < items:
            previous_response = self.client.get(initial_json['_links']['previous']['href'], name="time_minute")
            previous_json = get_response_as_json(previous_response)
            query_items.extend(previous_json['_links']['uris'])
        query_items.extend(initial_json['_links']['uris'])
        query_slice = query_items[-items:]
        total_time = int((time.time() - start_time) * 1000)
        if cmp(query_slice, posted_items) == 0:
            events.request_success.fire(request_type="sequential", name="compare", response_time=total_time, response_length=items)
        else:
            events.request_failure.fire(request_type="sequential", name="compare", response_time=total_time, exception='incorrect sequential items')
            logger.error('sequential | compare | incorrect items: ' + abbreviate(query_slice) + ' instead of ' + abbreviate(posted_items))

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
        first_response = self.client.get(url, name="time_minute")
        first_json = get_response_as_json(first_response)
        second_response = self.client.get(first_json['_links']['previous']['href'], name="time_minute")
        second_json = get_response_as_json(second_response)
        items.extend(second_json['_links']['uris'])
        items.extend(first_json['_links']['uris'])
        num_items = str(len(items) - 1)
        next_url = items[0] + "/next/" + num_items + "?stable=false"
        next_response = self.client.get(next_url, name="next")
        next_json = get_response_as_json(next_response)
        next_uris = next_json['_links']['uris']
        if cmp(next_uris, items[1:]) == 0:
            events.request_success.fire(request_type="next", name="compare", response_time=1, response_length=len(items))
        else:
            events.request_failure.fire(request_type="next", name="compare", response_time=1, exception='incorrect next items')
            logger.error('next | compare | incorrect items: ' + abbreviate(next_uris) + ' instead of ' + abbreviate(items[1:]))

        previous_url = items[-1] + "/previous/" + num_items + "?stable=false"
        previous_response = self.client.get(previous_url, name="previous")
        previous_json = get_response_as_json(previous_response)
        previous_uris = previous_json['_links']['uris']
        if cmp(previous_uris, items[:-1]) == 0:
            events.request_success.fire(request_type="previous", name="compare", response_time=1, response_length=len(items))
        else:
            events.request_failure.fire(request_type="previous", name="compare", response_time=1, exception='incorrect previous items')
            logger.error('previous | compare | incorrect items: ' + abbreviate(previous_uris) + ' instead of ' + abbreviate(items[:-1]))

    def second_query(self):
        results_response = self.client.get(self.time_path("second"), name="time_second")
        results_json = get_response_as_json(results_response)
        items = 60
        for x in range(0, items):
            results_response = self.client.get(results_json['_links']['previous']['href'], name="time_second")
            results_json = get_response_as_json(results_response)

    def time_path(self, unit="second"):
        return "/channel/" + self.channel + self.user.time_path(unit) + "?stable=false"

    def next(self, time_unit):
        path = self.time_path(time_unit)
        with self.client.get(path, catch_response=True, name="time_" + time_unit) as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code))

        links = get_response_as_json(postResponse)
        uris = links['_links']['uris']
        if len(uris) > 0:
            for uri in uris:
                self.read(uri)

    def next_10(self):
        utcnow = datetime.utcnow()
        self.do_next(utcnow + timedelta(minutes=-1))
        self.do_next(utcnow + timedelta(hours=-1))
        self.do_next(utcnow + timedelta(days=-1))

    def do_next(self, time):
        path = "/channel/" + self.channel + time.strftime("/%Y/%m/%d/%H/%M/%S/000") + "/A/next/10"
        with self.client.get(path, catch_response=True, name="next") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on next: " + str(postResponse.status_code))

    def payload_generator(self, chars=string.ascii_uppercase + string.digits):
        size = self.number * self.number * 300
        return ''.join(random.choice(chars) for x in range(size))

    def verify_callback(self, store, name="webhook", count=2000):
        store[self.channel]['lock'].acquire()
        items = len(store[self.channel]["data"])
        if items > count:
            events.request_failure.fire(request_type=name, name="length", response_time=1, exception='too many items')
            logger.error(name + ' | length | too many items: ' + self.channel + ' ' + str(items))
        else:
            events.request_success.fire(request_type=name, name="length", response_time=1, response_length=1)
        store[self.channel]['lock'].release()

    def verify_callback_length(self, count=2000):
        self.verify_callback(webhooks, "webhook", count)
        if self.user.has_websocket():
            if websockets[self.channel]["open"]:
                self.verify_callback(websockets, "websocket")
        if webhooks[self.channel]["heartbeat"]:
            heartbeats_ = webhooks[self.channel]["heartbeats"]
            if len(heartbeats_) > 2:
                events.request_failure.fire(request_type="heartbeats", name="length", response_time=1, exception='too many heartbeats')
                logger.error('hearbeats | length | too many heartbeats: ' + self.channel + ' ' + str(heartbeats_))

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
                logger.error(name + ' | ordered | item in wrong order: ' + incoming_uri + ' | found at ' + str(obj[channel]['data'].index(incoming_uri)))
                (obj[channel]["data"]).remove(incoming_uri)
            else:
                obj[channel]["unknown"].append(str(incoming_uri))
                events.request_failure.fire(request_type=name, name="ordered", response_time=1, exception='item unknown')
                logger.error(name + ' | ordered | item unknown: ' + incoming_uri)

    @staticmethod
    def verify_parallel(channel, incoming_uri):
        if incoming_uri in webhooks[channel]["data"]:
            (webhooks[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type="webhook", name="parallel", response_time=1, response_length=1)
        else:
            logger.error('webhook | parallel | item unknown: ' + incoming_uri)
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
                logger.warning("incoming uri before locust tests started " + str(incoming_uri))
                return
            try:
                content_key = HubTasks.get_short_path(incoming_uri)
                timestamp = get_item_timestamp(content_key)
                if timestamp < webhooks[channel]['start']:
                    logger.warning('item before start time: ' + content_key)
                    return

                webhooks[channel]['lock'].acquire()
                if webhooks[channel]["parallel"] == 1:
                    HubTasks.verify_ordered(channel, content_key, webhooks, "webhook")
                else:
                    HubTasks.verify_parallel(channel, content_key)
            finally:
                if not webhooks[channel]['lock'].locked():
                    logger.warning('no webhook lock to release for: ' + channel)
                else:
                    webhooks[channel]['lock'].release()

    @staticmethod
    def heartbeat(channel, incoming_json):
        if channel not in webhooks:
            logger.warning('no webhook store for: ' + channel)
            return

        heartbeats_ = webhooks[channel]["heartbeats"]
        id_ = incoming_json['id']

        if len(heartbeats_) > 0 and id_ == heartbeats_[0]:
            heartbeats_.remove(id_)
            events.request_success.fire(request_type="heartbeats", name="order", response_time=1, response_length=1)
        elif id_ != webhooks[channel]["lastHeartbeat"]:
            if id_ in heartbeats_:
                heartbeats_.remove(id_)
            events.request_success.fire(request_type="heartbeats", name="order", response_time=1, response_length=1)
        else:
            events.request_failure.fire(request_type="heartbeats", name="order", response_time=1, exception='heartbeat in wrong order')
            logger.error('heartbeats | order | heartbeat in wrong order: ' + id_ + ' | found at ' + str(heartbeats_.index(id_)))

        webhooks[channel]["lastHeartbeat"] = id_

    @staticmethod
    def get_store(name):
        return get_store_by_name(name)

    def http_get(self, url, headers=None, **kwargs):
        displayable_headers = headers if headers else {}
        logger.debug('GET > {} {}'.format(url, displayable_headers))
        response = self.client.get(url, headers=headers, **kwargs)
        status_message = requests.status_codes.codes.get(response.status_code)
        logger.debug('GET < {} {} {}'.format(url, response.status_code, status_message))
        return response

    def http_put(self, url, data, headers=None, **kwargs):
        displayable_headers = headers if headers else {}
        logger.debug('PUT > {} {} {}'.format(url, displayable_headers, data[:20]))
        response = self.client.put(url, headers=headers, data=data, **kwargs)
        status_message = requests.status_codes.codes.get(response.status_code)
        logger.debug('PUT < {} {} {}'.format(url, response.status_code, status_message))
        return response

    def http_post(self, url, data, headers=None, **kwargs):
        displayable_headers = headers if headers else {}
        logger.debug('POST > {} {} {}'.format(url, displayable_headers, data[:20]))
        response = self.client.post(url, headers=headers, data=data, **kwargs)
        status_message = requests.status_codes.codes.get(response.status_code)
        logger.debug('POST < {} {} {}'.format(url, response.status_code, status_message))
        return response

    def http_delete(self, url, headers=None, **kwargs):
        displayable_headers = headers if headers else {}
        logger.debug('DELETE > {} {}'.format(url, displayable_headers))
        response = self.client.delete(url, headers=headers, **kwargs)
        status_message = requests.status_codes.codes.get(response.status_code)
        logger.debug('DELETE < {} {} {}'.format(url, response.status_code, status_message))
        return response


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


def get_response_as_json(response):
    try:
        return response.json()
    except ValueError:
        logger.warning('invalid response: ' + str(response.status_code) + ' ' + response.text)
        raise


def abbreviate(array):
    return '[' + array[0] + ' ... ' + array[len(array) - 1] + ']'


def get_store_by_name(name):
    if name == 'webhooks':
        return webhooks
    elif name == 'websockets':
        return websockets
    else:
        raise ValueError(name)


def get_previous_url(url):
    response = requests.get(url + '/previous/1')
    response.raise_for_status()
    content = response.json()
    uris = content['_links']['uris']
    if len(uris) == 0:
        return None
    else:
        return uris[0]


def convert_http_to_ws_url(http_url):
    http_url = urlparse(http_url)
    return http_url._replace(scheme='ws').geturl() + '/ws'
