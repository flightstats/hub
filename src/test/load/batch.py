import logging
from locust import HttpLocust, TaskSet, task, web
from flask import request, Response

from hubTasks import HubTasks, get_response_as_json
from hubUser import HubUser
from log import setup_logging
import utils

setup_logging('/mnt/log/batch.log')
logger = logging.getLogger(__name__)


class BatchUser(HubUser):
    def name(self):
        return "batch_test_"

    def start_channel(self, payload, tasks):
        payload["storage"] = "BATCH"

    def start_webhook(self, config):
        # First User - create channel - posts to channel, parallel group callback on channel
        # Second User - create channel - posts to channel, parallel group callback on channel
        # Third User - create channel - posts to channel, minute group callback on channel
        config['parallel'] = 10
        config['batch'] = "SINGLE"
        if config['number'] == 3:
            config['parallel'] = 1
            config['batch'] = "MINUTE"


class VerifierTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(BatchUser(), self.client)
        self.hubTasks.start()

    @task(10000)
    def write(self):
        bulk = ""
        for x in range(0, 50):
            bulk += "--abcdefg\r\n"
            bulk += "Content-Type: application/json\r\n\r\n"
            bulk += '{"name":"' + self.hubTasks.payload + '", "count": ' + str(self.hubTasks.count) + '}\r\n'
            self.hubTasks.count += 1
        bulk += "--abcdefg--\r\n"

        with self.client.post("/channel/" + self.hubTasks.channel + "/bulk", data=bulk,
                              headers={"Content-Type": "multipart/mixed; boundary=abcdefg"}, catch_response=True,
                              name="post_bulk") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = get_response_as_json(postResponse)
        logger.debug('item POSTed: ' + links['_links']['self']['href'])

        uris = links['_links']['uris']
        for uri in uris:
            self.hubTasks.append_href(uri, 'websockets')
            self.hubTasks.append_href(uri, 'webhooks')
        # todo add read functionality
        return uris

    @task(10)
    def next_10(self):
        self.hubTasks.next_10()

    @task(10)
    def minute_query(self):
        self.hubTasks.minute_query()

    @task(10)
    def second_query(self):
        self.hubTasks.second_query()

    @task(10)
    def verify_callback_length(self):
        self.hubTasks.verify_callback_length(50000)

    @web.app.route("/callback", methods=['GET'])
    def get_channels():
        logger.debug(utils.get_client_address(request) + ' | ' + request.method + ' | /callback')
        return HubTasks.get_channels()

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        logger.debug(utils.get_client_address(request) + ' | ' + request.method + ' | /callback/' + channel + ' | ' + request.get_data().strip())
        return HubTasks.callback(channel)

    @web.app.route('/store/<name>', methods=['GET'])
    def get_store(name):
        return Response(HubTasks.get_store(name), mimetype='application/json')


class WebsiteUser(HttpLocust):
    task_set = VerifierTasks
    min_wait = 600
    max_wait = 1000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
