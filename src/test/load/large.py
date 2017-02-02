# locust.py
from locust import HttpLocust, TaskSet, task, web

from hubTasks import HubTasks
from hubUser import HubUser


class LargeUser(HubUser):
    def name(self):
        return "large_test_"


class VerifierTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(LargeUser(), self.client)
        self.hubTasks.start()

    @task(100)
    def write_read(self):
        self.hubTasks.write_read()

    @task(1)
    def sequential(self):
        self.hubTasks.sequential()

    @task(1)
    def earliest(self):
        self.hubTasks.earliest()

    @task(1)
    def latest(self):
        self.hubTasks.latest()

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

    @task(1)
    def next_previous(self):
        self.hubTasks.next_previous()

    @task(1)
    def second_query(self):
        self.hubTasks.second_query()

    @task(10)
    def verify_callback_length(self):
        self.hubTasks.verify_callback_length()

    @web.app.route("/callback", methods=['GET'])
    def get_channels():
        return HubTasks.get_channels()

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        return HubTasks.callback(channel)


class WebsiteUser(HttpLocust):
    task_set = VerifierTasks
    min_wait = 400 * 1000
    max_wait = 600 * 1000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
