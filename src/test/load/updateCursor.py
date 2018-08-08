import logging
from locust import HttpLocust, TaskSet, task, web
from flask import request, Response

from hubTasks import HubTasks
from hubUser import HubUser

from log import setup_logging
import utils

setup_logging('/mnt/log/updateCursor.log')
logger = logging.getLogger(__name__)


class UpdateCursorUser(HubUser):
    def name(self):
        return "update_cursor_test_"

    def start_channel(self, payload, tasks):
        payload["storage"] = "SINGLE"

    def start_webhook(self, config):
        config['parallel'] = 1
        config['batch'] = "SECOND"
        config['heartbeat'] = True

    # this test messes with the order of the webhook by design - so turning off this test
    def skip_verify_ordered(self):
        return True


class UpdateCursorTasks(TaskSet):
    hubTasks = None

    def on_start(self):
        self.hubTasks = HubTasks(UpdateCursorUser(), self.client)
        self.hubTasks.start()

    @task(1000)
    def write_read(self):
        self.hubTasks.write_read()

    @task(50)
    def verify_cursor_update(self):
        self.hubTasks.verify_cursor_update()

    @task(50)
    def verify_cursor_update_via_upsert(self):
        self.hubTasks.verify_cursor_update_via_upsert()

    @task(10)
    def get_webhook_config(self):
        self.hubTasks.get_webhook_config()

    @web.app.route("/callback", methods=['GET'])
    def get_channels(self):
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
    task_set = UpdateCursorTasks
    min_wait = 100
    max_wait = 1000

    def __init__(self):
        super(WebsiteUser, self).__init__()
        HubTasks.host = self.host
