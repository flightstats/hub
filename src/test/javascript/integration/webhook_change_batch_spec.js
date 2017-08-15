require('../integration_config');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'SINGLE'
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    var webhookConfig2 = {
        callbackUrl: 'http://nothing/callback',
        channelUrl: 'http://nothing/channel/notHere',
        batch: 'MINUTE'
    };

    utils.putWebhook(webhookName, webhookConfig2, 200, testName);

    utils.deleteWebhook(webhookName);

});

