require('../integration_config');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfigA = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 1,
    batch: 'SINGLE'

};

var webhookConfigB = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 2,
    batch: 'SINGLE'

};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfigA, 201, testName);

    utils.getWebhook(webhookName, webhookConfigA);

    utils.putWebhook(webhookName, webhookConfigB, 200, testName);

    utils.getWebhook(webhookName, webhookConfigB);

});

