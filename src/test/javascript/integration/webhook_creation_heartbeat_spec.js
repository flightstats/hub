require('../integration_config');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false
};

var webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false,
    batch: 'SINGLE',
    parallelCalls: 1
};

var webhookConfig3 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: true,
    batch: 'SINGLE',
    parallelCalls: 1
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.getWebhook(webhookName, webhookConfig2);

    utils.putWebhook(webhookName, webhookConfig3, 200, testName);

    utils.getWebhook(webhookName, webhookConfig3);

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig3, 404);
});

