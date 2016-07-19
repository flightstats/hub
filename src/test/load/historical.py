# locust.py
from datetime import datetime, timedelta
from locust import HttpLocust, TaskSet, task

from hubTasks import HubTasks
from hubUser import HubUser


class HistoricalUser(HubUser):
    def name(self):
        return "historical_test_"

    def channel_payload(self, payload):
        payload["historical"] = "true"

    def channel_post_url(self, channel):
        return "/channel/" + channel + "/" + self.historical_time("%Y/%m/%d/%H/%M/%S/") + "001"

    def has_webhook(self):
        return False

    def has_websocket(self):
        return False

    def time_path(self, unit="second"):
        if unit == "hour":
            return self.historical_time("/%Y/%m/%d/%H")
        if unit == "minute":
            return self.historical_time("/%Y/%m/%d/%H/%M")
        if unit == "second":
            return self.historical_time("/%Y/%m/%d/%H/%M/%S")

    def historical_time(self, time_format):
        return (datetime.utcnow() - timedelta(days=1)).strftime(time_format)


class HistoricalTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.user = HistoricalUser()
        self.hubTasks = HubTasks(self.user, self.client)
        self.hubTasks.start()

    @task(100)
    def write_read(self):
        self.hubTasks.write_read()

    @task(1)
    def hour_query(self):
        self.hubTasks.hour_query()

    @task(1)
    def hour_query_get_items(self):
        self.hubTasks.hour_query_get_items()

    @task(1)
    def minute_query(self):
        self.hubTasks.minute_query()

    @task(1)
    def minute_query_get_items(self):
        self.hubTasks.minute_query_get_items()

    @task(10)
    def second_query(self):
        self.hubTasks.second_query()

    @task(10)
    def next_previous(self):
        self.hubTasks.next_previous()

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
