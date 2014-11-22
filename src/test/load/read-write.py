# locust.py

import json
import string
import random

from locust import HttpLocust, TaskSet, task, events

# Usage:
# locust -f read-write.py -H http://hub-v2.svc.dev
# nohup locust -f read-write.py -H http://hub-v2.svc.dev &

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
                         headers={"Content-Type": "application/json"}
        )

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        with self.client.post("/channel/" + self.channel, data=json.dumps(payload),
                              headers={"Content-Type": "application/json"}, catch_response=True) as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = postResponse.json()
        self.count += 1
        return links['_links']['self']['href']

    @task(1000)
    def write_read(self):
        self.client.get(self.write(), name="get_payload")

    @task(10)
    def sequential(self):
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
        if cmp(query_slice, posted_items) == 0:
            events.request_success.fire(request_type="sequential", name="compare", response_time=0,
                                        response_length=items)
        else:
            print "expected " + ", ".join(posted_items) + " found " + ", ".join(query_slice)
            events.request_failure.fire(request_type="sequential", name="compare", response_time=0, exception=-1)

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

        # add a test which puts in 10 items sequentially, then verfies that the items are still in the same order


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 400
    max_wait = 900
