# locust.py

from locust import HttpLocust, TaskSet, task, web

from hubTasks import HubTasks
from hubUser import HubUser


class UpdateCursorUser(HubUser):
    def name(self):
        return "single_test_"

    def start_channel(self, payload, tasks):
        payload["storage"] = "SINGLE"

    def start_webhook(self, config):
        config['parallel'] = 1
        config['batch'] = "SECOND"
        config['heartbeat'] = True


class UpdateCursorTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(UpdateCursorUser(), self.client)
        self.hubTasks.start()

    @task(1000)
    def write_read(self):
        self.hubTasks.write_read()

    @task(10)
    def update_cursor(self):
        self.hubTasks.update_webhook()

    @task(10)
    def get_webhook_config(self):
        self.hubTasks.get_webhook_config()

    # @task(10)
    # def hour_query(self):
    #     self.hubTasks.hour_query()
    #
    # @task(10)
    # def minute_query(self):
    #     self.hubTasks.minute_query()
    #
    # @task(10)
    # def second_query(self):
    #     self.hubTasks.second_query()
    #
    # @task(10)
    # def next_previous(self):
    #     self.hubTasks.next_previous()
    #
    # @task(10)
    # def verify_callback_length(self):
    #     self.hubTasks.verify_callback_length(20000)

    @web.app.route("/callback", methods=['GET'])
    def get_channels(self):
        return HubTasks.get_channels()

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        return HubTasks.callback(channel)


class WebsiteUser(HttpLocust):
    task_set = UpdateCursorTasks
    min_wait = 1
    max_wait = 3

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
