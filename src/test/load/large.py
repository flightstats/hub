import json
import logging
import os
import random
import string
from locust import HttpLocust, TaskSet, task, web
from flask import request, Response

from hubTasks import HubTasks
from hubUser import HubUser

logger = logging.getLogger(__name__)


class LargeUser(HubUser):
    def name(self):
        return "large_test_"

    def start_channel(self, payload, tasks):
        pass

    def start_webhook(self, config):
        if config['number'] == 1:
            config['webhook_channel'] = config['channel'] + "_replicated"
            url = "/channel/" + config['webhook_channel']
            headers = {"Content-Type": "application/json"}
            channel_config = {
                "name": config['webhook_channel'],
                "ttlDays": "3",
                "replicationSource": config['host'] + "/channel/" + config['channel']
            }
            config['client'].put(url, data=json.dumps(channel_config), headers=headers, name="replication")

    def has_webhook(self):
        return True

    def has_websocket(self):
        return False


class LargeTasks(TaskSet):
    hubTasks = None
    first = True

    def on_start(self):
        self.hubTasks = HubTasks(LargeUser(), self.client)
        self.hubTasks.start()

    def large_file_name(self, number, direction):
        return '/mnt/large' + str(number) + '.' + direction

    # todo have this create a replicated channel

    @task(100)
    def write(self):
        if self.first:
            self.create_large(self.hubTasks)
            self.first = False
        large_file_name = self.large_file_name(self.hubTasks.number, 'out')
        large_file = open(large_file_name, 'rb')
        expected_size = os.stat(large_file_name).st_size
        threads = "8"
        with self.hubTasks.client.post(self.hubTasks.get_channel_url() + "?threads=" + threads,
                                       data=large_file,
                                       headers={"content-type": "application/octet-stream"},
                                       catch_response=True,
                                       name="post_payload_" + threads) as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code)
                                     + self.hubTasks.get_channel_url())
            else:
                uri = postResponse.json()['_links']['self']['href']
                with self.client.get(uri, stream=True, catch_response=True, name="get_payload") as getResponse:
                    if getResponse.status_code != 200:
                        getResponse.failure("Got wrong response on get: " + str(getResponse.status_code) + " " + uri)
                    inputFile = self.large_file_name(self.hubTasks.number, 'in')
                    with open(inputFile, 'wb') as fd:
                        for chunk in getResponse.iter_content(chunk_size=1024):
                            if chunk:
                                fd.write(chunk)
                    get_size = os.stat(inputFile).st_size
                    if get_size == expected_size:
                        logger.info("Got expected size on get: " + str(get_size) + " " + uri)
                    else:
                        getResponse.failure("Got wrong size on get: " + str(get_size) + " " + uri)

    def create_large(self, tasks):
        large_file_name = self.large_file_name(self.hubTasks.number, 'out')
        if os.path.isfile(large_file_name):
            logger.info("using existing file " + large_file_name + " bytes=" + str(os.stat(large_file_name).st_size))
            return
        if tasks.number == 1:
            target = open(large_file_name, 'w')
            logger.info("writing file " + large_file_name)
            target.truncate(0)
            chars = string.ascii_uppercase + string.digits
            size = 50 * 1024
            for x in range(0, 10 * 1024):
                target.write(''.join(random.choice(chars) for i in range(size)))
                target.flush()

            logger.info("closing " + large_file_name)
            target.close()
        elif tasks.number == 2:
            os.system("cat /mnt/large1.out /mnt/large1.out > /mnt/large2.out")
        elif tasks.number == 3:
            os.system("cat /mnt/large2.out /mnt/large2.out > /mnt/large3.out")

    @task(10)
    def verify_callback_length(self):
        self.hubTasks.verify_callback_length()

    @web.app.route("/callback", methods=['GET'])
    def get_channels():
        logger.debug(request.remote_addr + ' | ' + request.method + ' | /callback')
        return HubTasks.get_channels()

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        logger.debug(request.remote_addr + ' | ' + request.method + ' | /callback/' + channel + ' | ' + request.get_data().strip())
        return HubTasks.callback(channel)

    @web.app.route('/store/<name>', methods=['GET'])
    def get_store(name):
        return Response(HubTasks.get_store(name), mimetype='application/json')


class WebsiteUser(HttpLocust):
    task_set = LargeTasks
    min_wait = 5000
    max_wait = 30000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
