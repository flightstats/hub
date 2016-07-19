# locust.py

from locust import HttpLocust, TaskSet, task, web

from hubTasks import BasicTasks


# todo do we need this here instead?
# basicTasks =

class VerifierUser(TaskSet):
    basicTasks = None

    def name(self):
        return "verifier_test_"

    def on_start(self):
        self.basicTasks = BasicTasks(self)
        self.basicTasks.on_start()

    @task(1000)
    def write_read(self):
        self.basicTasks.write_read()

    @task(10)
    def change_parallel(self):
        self.basicTasks.change_parallel()

    @task(100)
    def sequential(self):
        self.basicTasks.sequential()

    @task(1)
    def hour_query(self):
        self.basicTasks.hour_query()

    @task(1)
    def hour_query_get_items(self):
        self.basicTasks.hour_query_get_items()

    @task(1)
    def minute_query(self):
        self.basicTasks.minute_query()

    @task(1)
    def minute_query_get_items(self):
        self.basicTasks.minute_query_get_items()

    @task(10)
    def next_previous(self):
        self.basicTasks.next_previous()

    @task(10)
    def second_query(self):
        self.basicTasks.second_query()

    @task(10)
    def verify_callback_length(self):
        self.basicTasks.verify_callback_length()

    @web.app.route("/callback", methods=['GET'])
    def get_channels():
        return BasicTasks.get_channels()

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        return BasicTasks.callback(channel)


class WebsiteUser(HttpLocust):
    task_set = VerifierUser
    min_wait = 500
    max_wait = 5000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        BasicTasks.host = self.host
