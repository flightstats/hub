# locust.py
import json
from locust import HttpLocust, TaskSet, task, web

from hubTasks import HubTasks
from hubUser import HubUser


class VerifierUser(HubUser):
    def name(self):
        return "verifier_test_"

    def start_webhook(self, config):
        # First - posts to channel, webhook on channel
        # Second - posts to channel, parallel webhook on channel
        # Third - posts to channel, replicate channel, webhook on replicated channel
        if config['number'] == 2:
            config['parallel'] = 2
        if config['number'] == 3:
            config['webhook_channel'] = config['channel'] + "_replicated"
            config['client'].put("/channel/" + config['webhook_channel'],
                                 data=json.dumps({"name": config['webhook_channel'], "ttlDays": "3",
                                                  "replicationSource": config['host'] + "/channel/" + config[
                                                      'channel']}),
                                 headers={"Content-Type": "application/json"},
                                 name="replication")


class VerifierTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(VerifierUser(), self.client)
        self.hubTasks.start()

    @task(1000)
    def write_read(self):
        self.hubTasks.write_read()

    @task(5)
    def change_parallel(self):
        self.hubTasks.change_parallel("verifier_test_2")

    @task(100)
    def sequential(self):
        self.hubTasks.sequential()

    @task(1)
    def earliest(self):
        self.hubTasks.earliest()

    @task(100)
    def latest(self):
        self.hubTasks.latest()

    @task(10)
    def day_query(self):
        self.hubTasks.day_query()

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
    def next_previous(self):
        self.hubTasks.next_previous()

    @task(10)
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
    min_wait = 500
    max_wait = 5000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
