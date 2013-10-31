# locust.py

import json, uuid
from locust import Locust, TaskSet, task
import sys
import getopt
import websocket
import httplib2
import time, thread

# Usage:
# locust -f read-write.py -H http://datahub.svc.prod

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
        thread.start_new_thread ( self.reader, () )

    @task
    def index(self):

        payload = {"name": "stuff", "count": self.count}
        self.client.post("/channel/" + self.channel,
                         data=json.dumps(payload),
                         headers = {"Content-Type": "application/json"}
        )
        self.count += 1

    def reader(self):
        time.sleep(5)
        self._http = httplib2.Http()
        meta = self._load_metadata()
        ws_uri = meta['_links']['ws']['href']
        print ws_uri
        ws = websocket.WebSocketApp(ws_uri,on_message = self.on_message)
        ws.run_forever()

    def on_message(self, ws, message):
        r, c = self._http.request(message, 'GET')
        #print("--%s" %(c))

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request( self.client.base_url + "/channel/" + self.channel, 'GET')
        return json.loads(c)


class WebsiteUser(Locust):
    task_set = WebsiteTasks
    min_wait = 1000
    max_wait = 2000
