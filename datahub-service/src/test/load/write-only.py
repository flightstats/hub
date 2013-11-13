# locust.py

import json, uuid
from locust import Locust, TaskSet, task

# Usage:
# locust -f write-only.py -H http://datahub.svc.prod

# Be sure to update gevent to get around DNS issue
# pip install https://github.com/surfly/gevent/releases/download/1.0rc3/gevent-1.0rc3.tar.gz

class WebsiteTasks(TaskSet):



    def on_start(self):
        self.channel = "test" + uuid.uuid1().hex
        self.count = 0
        payload = {"name": self.channel,"ttlMillis": "36000000"}
        self.client.post("/channel",
                         data=json.dumps(payload),
                         headers = {"Content-Type": "application/json"}
        )

    @task
    def index(self):

        payload = {"name": "stuff", "count": self.count}
        self.client.post("/channel/" + self.channel,
                         data=json.dumps(payload),
                         headers = {"Content-Type": "application/json"}
        )
        self.count += 1




class WebsiteUser(Locust):
    task_set = WebsiteTasks
    min_wait = 1000
    max_wait = 2000
