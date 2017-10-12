require('../integration_config');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere'
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    var webhookConfig2 = {
        callbackUrl : 'http://nothing/callback2',
        channelUrl : 'http://different/channel/notHere'
    };

    // This is ok because even though the channel URL's are different, the "channelName" is the same
    utils.putWebhook(webhookName, webhookConfig2, 200, testName);

    var webhookConfig3 = {
        callbackUrl: 'http://nothing/callback2',
        channelUrl: 'http://different/channel/not_Here'
    };

    // the "_" changes the actual channel name - which causes the rejection
    utils.putWebhook(webhookName, webhookConfig3, 409, testName);

    utils.deleteWebhook(webhookName);

});

