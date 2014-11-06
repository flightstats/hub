# locust.py

import json
import string
import random

from locust import HttpLocust, TaskSet, task
import httplib2

# Usage:
# locust -f read-write.py -H http://hub.svc.prod
# nohup locust -f read-write.py -H http://hub-v2.svc.dev &

class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        self._http = httplib2.Http()
        WebsiteTasks.channelNum += 1
        self.number = WebsiteTasks.channelNum * 2000
        self.payload = self.payload_generator(self.number)
        print("payload size " + str(self.payload.__sizeof__()))
        self.channel = "riak_test_" + str(WebsiteTasks.channelNum)
        self.count = 0
        payload = {"name": self.channel, "ttlDays": "100"}
        self.client.post("/channel",
                         data=json.dumps(payload),
                         headers={"Content-Type": "application/json"}
        )

    @task(100)
    def write_read(self):
        payload = {"name": self.payload, "count": self.count}
        #write payload
        with self.client.post("/channel/" + self.channel, data=json.dumps(payload),
                              headers={"Content-Type": "application/json"}, catch_response=True) as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + postResponse.status_code)

        links = postResponse.json()
        getResponse = self._http.request(links['_links']['self']['href'], 'GET')
        # print "Get Response status code:", getResponse
        if getResponse[0]['status'] != '200':
            postResponse.failure("Got wrong status on get: " + getResponse[0]['status'])
        else:
            body = json.loads(getResponse[1])
            if body['count'] != self.count:
                postResponse.failure("wrong count: " + str(body['count']))

        self.count += 1

    @task(1)
    def day_query(self):
        self.next(self.time_path("day"))

    @task(5)
    def hour_query(self):
        self.next(self.time_path("hour"))

    @task(10)
    def minute_query(self):
        self.next(self.time_path("minute"))

    @task(10)
    def second_query(self):
        self.client.get(self.time_path("second"))

    def time_path(self, unit="second"):
        return "/channel/" + self.channel + "/time/" + unit

    def next(self, path):
        # todo - this should use _http instead, and store stats using
        #events.request_failure.fire(request_type="xmlrpc", name=name, response_time=total_time, exception=e)
        # events.request_success.fire(request_type="xmlrpc", name=name, response_time=total_time, response_length=0)
        # from http://docs.locust.io/en/latest/testing-other-systems.html
        # this will prevent unique URIs being reported by locust
        with self.client.get(path, catch_response=True) as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + postResponse.status_code)
        links = postResponse.json()
        uris = links['_links']['uris']
        #todo - maybe loop over all the results?
        if len(uris) > 0:
            self_href = uris[0]
            # todo - this should use _http instead
            self.client.get(self_href)


    def payload_generator(self, size, chars=string.ascii_uppercase + string.digits):
        return ''.join(random.choice(chars) for x in range(size))


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 500
    max_wait = 1000
