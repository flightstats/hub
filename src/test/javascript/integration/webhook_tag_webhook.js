require('../integration_config');

var request = require('request');
var http = require('http');
var testName = __filename;
var tag = utils.randomTag();
var webhookName = tag;
var webhookConfig = {
    'callbackUrl': 'http://nothing/callback',
    'channelUrl': hubUrlBase,
    'tag': tag
};

var channelName1 = utils.randomChannelName();
var channelName2 = utils.randomChannelName();

var channel1 = {
    'tags': [tag],
    'name': channelName1
}

var channel2 = {
    'tags': [tag],
    'name': channelName2
}

var instancePrefix = 'TAGWH_' + tag;
var instance1 = instancePrefix + "_" + channelName1;
var instance2 = instancePrefix + "_" + channelName1;

var verify = function (parse) {
    return true;
}

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.createChannelWithConfig(channelName1, channel1);
    utils.createChannelWithConfig(channelName2, channel2);
    utils.itSleeps(1000);
    // look for 2 webhooks to be created
    utils.getWebhook(instance1, webhookConfig, 200, verify);
    utils.getWebhook(instance2, webhookConfig, 200, verify);

    // //update the channel to remove tag (and as side effect remove it's tag webhook instance)
    var channel1noTag = {
        'name': channelName1,
        'tags': [],
    }
    utils.createChannelWithConfig(channelName1, channel1noTag);
    utils.itSleeps(1000);
    // the webhook associated with channelName1 should go away because of the removed tag
    utils.getWebhook(instance1, webhookConfig, 404);

    // delete webhook (and as side effect remove remaining webhook
    utils.deleteWebhook(webhookName);
    utils.itSleeps(1000);
    utils.getWebhook(instance2, webhookConfig, 404);

});

