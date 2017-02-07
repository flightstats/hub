# locust.py
from locust import HttpLocust, TaskSet, task, web

from hubTasks import HubTasks
from hubUser import HubUser


# locust -f large.py --host=http://localhost:8080

# todo figure out which tests are appropriate
class LargeUser(HubUser):
    def name(self):
        return "large_test_"

    def start_webhook(self, config):
        pass

    def has_webhook(self):
        return False

    def has_websocket(self):
        return False


class LargeTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        # create a large file
        self.hubTasks = HubTasks(LargeUser(), self.client)
        self.hubTasks.start()

    @task(100)
    def write_read(self):
        # how do we use a large paylod?
        large_file = open('large.dmg', 'rb')
        r = self.hubTasks.client.post(self.hubTasks.get_channel_url(),
                                      data=large_file,
                                      headers={'content-type': 'application/octet-stream'})

    # @task(1)
    # def sequential(self):
    #     self.hubTasks.sequential()
    #
    # @task(1)
    # def earliest(self):
    #     self.hubTasks.earliest()
    #
    # @task(1)
    # def latest(self):
    #     self.hubTasks.latest()
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
    # @task(1)
    # def next_previous(self):
    #     self.hubTasks.next_previous()
    #
    # @task(1)
    # def second_query(self):
    #     self.hubTasks.second_query()
    #
    # @task(1)
    # def verify_callback_length(self):
    #     self.hubTasks.verify_callback_length()

    @web.app.route("/callback", methods=['GET'])
    def get_channels():
        return HubTasks.get_channels()

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        return HubTasks.callback(channel)


class WebsiteUser(HttpLocust):
    task_set = LargeTasks
    min_wait = 500
    max_wait = 5000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
