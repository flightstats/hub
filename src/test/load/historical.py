# locust.py

import time
from locust import HttpLocust, TaskSet, task

from hubTasks import HubTasks
from hubUser import HubUser


class HistoricalUser(HubUser):
    def name(self):
        return "historical_test_"

    def channel_payload(self, payload):
        payload["historical"] = "true"


class HistoricalTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(HistoricalUser(), self.client)
        self.hubTasks.on_start()
        # self.hubTasks.start_websocket()
        # self.hubTasks.start_group_callback()
        time.sleep(5)

    @task(1000)
    def write_read(self):
        self.hubTasks.write_read()

        # @task(10)
        # def change_parallel(self):
        #     self.hubTasks.change_parallel()
        #
        # @task(100)
        # def sequential(self):
        #     self.hubTasks.sequential()
        #
        # @task(1)
        # def hour_query(self):
        #     self.hubTasks.hour_query()
        #
        # @task(1)
        # def hour_query_get_items(self):
        #     self.hubTasks.hour_query_get_items()
        #
        # @task(1)
        # def minute_query(self):
        #     self.hubTasks.minute_query()
        #
        # @task(1)
        # def minute_query_get_items(self):
        #     self.hubTasks.minute_query_get_items()
        #
        # @task(10)
        # def next_previous(self):
        #     self.hubTasks.next_previous()
        #
        # @task(10)
        # def second_query(self):
        #     self.hubTasks.second_query()
        #
        # @task(10)
        # def verify_callback_length(self):
        #     self.hubTasks.verify_callback_length()
        #
        # @web.app.route("/callback", methods=['GET'])
        # def get_channels():
        #     return HubTasks.get_channels()
        #
        # @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
        # def callback(channel):
        #     return HubTasks.callback(channel)


class WebsiteUser(HttpLocust):
    task_set = HistoricalTasks
    min_wait = 500
    max_wait = 5000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
