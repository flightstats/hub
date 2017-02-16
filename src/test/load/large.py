# locust.py
import os
import random
import string
from locust import HttpLocust, TaskSet, task

from hubTasks import HubTasks
from hubUser import HubUser


# locust -f large.py --host=http://localhost:8080

# todo figure out which tests are appropriate
class LargeUser(HubUser):
    def name(self):
        return "large_test_"

    def start_channel(self, payload, tasks):
        self.create_large(tasks)

    def start_webhook(self, config):
        pass

    def has_webhook(self):
        return False

    def has_websocket(self):
        return False

    def create_large(tasks):
        size = 50 * 1024
        loops = 10 * 1024
        total_size = size * loops
        if tasks.number == 2:
            total_size = size * loops * 2
        elif tasks.number == 3:
            total_size = size * loops * 4

        large_file = 'large' + str(tasks.number) + '.out'
        statinfo = os.stat(large_file)
        if total_size == statinfo.st_size:
            print "existing " + large_file + " is " + str(total_size)
            return

        if tasks.number == 1:
            target = open(large_file, 'w')
            print "writing file " + large_file
            target.truncate(0)
            chars = string.ascii_uppercase + string.digits
            for x in range(0, loops):
                target.write(''.join(random.choice(chars) for i in range(size)))
                target.flush()
            print "closing " + large_file
            target.close()
        elif tasks.number == 2:
            os.system("cat large1.out large1.out > large2.out")
        elif tasks.number == 3:
            os.system("cat large2.out large2.out > large3.out")


class LargeTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(LargeUser(), self.client)
        self.hubTasks.start()

    @task(100)
    def write(self):
        large_file = open('large' + str(self.hubTasks.number) + '.out', 'rb')
        with self.hubTasks.client.post(self.hubTasks.get_channel_url(),
                                       data=large_file,
                                       headers={"content-type": "application/octet-stream"},
                                       catch_response=True,
                                       name="post_payload") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))


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


class WebsiteUser(HttpLocust):
    task_set = LargeTasks
    min_wait = 5000
    max_wait = 60000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
