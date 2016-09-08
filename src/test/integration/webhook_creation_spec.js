require('./integration_config.js');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere'
};

var webhookConfig2 = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere',
    parallelCalls: 1,
    batch: 'SINGLE'
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.getWebhook(webhookName, webhookConfig2);

    utils.putWebhook(webhookName, webhookConfig2, 200, testName);

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig2, 404);
});

