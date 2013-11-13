# locust.py

import json
import uuid
from locust import Locust, TaskSet, task
import websocket
import httplib2
import time
import thread
import string
import random


# Usage:
# locust -f read-write.py -H http://datahub.svc.prod

# Be sure to update gevent to get around DNS issue
# pip install https://github.com/surfly/gevent/releases/download/1.0rc3/gevent-1.0rc3.tar.gz

#todo variable timing from command line

class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        WebsiteTasks.channelNum += 1
        #todo make byte size this a command line var
        self.number = WebsiteTasks.channelNum * 100
        self.payload = self.payload_generator(self.number)
        print("payload size " + str(self.payload.__sizeof__()))
        self.channel = "test" + str(WebsiteTasks.channelNum)
        self.count = 0
        payload = {"name": self.channel, "ttlMillis": "36000000"}
        self.client.post("/channel",
                         data=json.dumps(payload),
                         headers={"Content-Type": "application/json"}
        )

        thread.start_new_thread(self.reader, ())

    @task
    def index(self):
        payload = {"name": self.payload, "count": self.count}
        self.client.post("/channel/" + self.channel,
                         data=json.dumps(payload),
                         headers={"Content-Type": "application/json"}
        )
        self.count += 1

    def reader(self):
        time.sleep(5)
        self._http = httplib2.Http()
        meta = self._load_metadata()
        ws_uri = meta['_links']['ws']['href']
        print ws_uri
        ws = websocket.WebSocketApp(ws_uri, on_message=self.on_message)
        ws.run_forever()

    def on_message(self, ws, message):
        r, c = self._http.request(message, 'GET')
        #print("--%s" %(c))

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self.client.base_url + "/channel/" + self.channel, 'GET')
        return json.loads(c)

    def payload_generator(self, size, chars=string.ascii_uppercase + string.digits):
        return ''.join(random.choice(chars) for x in range(size))


class WebsiteUser(Locust):
    task_set = WebsiteTasks
    min_wait = 1000
    max_wait = 2000
