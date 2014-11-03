# locust.py

import json
import string
import random

from locust import HttpLocust, TaskSet, task
import httplib2

# Usage:
# locust -f read-write.py -H http://hub.svc.prod

class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        self._http = httplib2.Http()
        WebsiteTasks.channelNum += 1
        #todo make byte size this a command line var
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

    @task
    def index(self):
        payload = {"name": self.payload, "count": self.count}
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


    def payload_generator(self, size, chars=string.ascii_uppercase + string.digits):
        return ''.join(random.choice(chars) for x in range(size))


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 500
    max_wait = 1000
