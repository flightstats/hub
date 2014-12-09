# locust.py

import json
import string
import random
import time

from locust import HttpLocust, TaskSet, task, events, web
from flask import request, jsonify





# Usage:
# locust -f read-write-group.py -H http://localhost:9080
# locust -f read-write-group.py -H http://hub-v2.svc.dev
# nohup locust -f read-write-group.py -H http://hub-v2.svc.dev &

groupCallbacks = {}


class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        WebsiteTasks.channelNum += 1
        self.number = WebsiteTasks.channelNum * WebsiteTasks.channelNum * 300
        self.payload = self.payload_generator(self.number)
        print("payload size " + str(self.payload.__sizeof__()))
        self.channel = "load_test_" + str(WebsiteTasks.channelNum)
        self.count = 0
        payload = {"name": self.channel, "ttlDays": "100"}
        self.client.post("/channel",
                         data=json.dumps(payload),
                         headers={"Content-Type": "application/json"})
        group_name = "/group/locust_" + self.channel
        self.client.delete(group_name)
        groupCallbacks[self.channel] = []
        # fix these urls
        group = {
            "callbackUrl": "http://localhost:8089/callback/" + self.channel,
            "channelUrl": "http://localhost:9080/channel/" + self.channel,
            "parallelCalls": 1
        }
        self.client.put(group_name,
                        data=json.dumps(group),
                        headers={"Content-Type": "application/json"})

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        with self.client.post("/channel/" + self.channel, data=json.dumps(payload),
                              headers={"Content-Type": "application/json"}, catch_response=True) as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = postResponse.json()
        self.count += 1
        href = links['_links']['self']['href']
        groupCallbacks[self.channel].append(href)
        return href

    @task(1000)
    def write_read(self):
        self.client.get(self.write(), name="get_payload")

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
            print "expected " + ", ".join(posted_items) + " found " + ", ".join(query_slice)
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
    def second_query(self):
        results = self.client.get(self.time_path("second"), name="time_second").json()
        items = 60
        for x in range(0, items):
            results = self.client.get(results['_links']['previous']['href'], name="time_second").json()

    def time_path(self, unit="second"):
        return "/channel/" + self.channel + "/time/" + unit

    def next(self, time_unit):
        path = self.time_path(time_unit)
        with self.client.get(path, catch_response=True, name="time_" + time_unit) as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code))
        links = postResponse.json()
        uris = links['_links']['uris']
        if len(uris) > 0:
            for uri in uris:
                self.client.get(uri, name="get_payload")

    def payload_generator(self, size, chars=string.ascii_uppercase + string.digits):
        return ''.join(random.choice(chars) for x in range(size))

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        if request.method == 'POST':
            if groupCallbacks[channel][0] == request.get_json()['uris'][0]:
                groupCallbacks[channel].pop(0)
            return "ok"
        else:
            return jsonify(items=groupCallbacks[channel])

    @web.app.route("/callbacks", methods=['GET'])
    def callbacks():
        return jsonify(groups=groupCallbacks)

        # how do we tie into Flask's reset?


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 400
    max_wait = 900

