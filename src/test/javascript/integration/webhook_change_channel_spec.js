require('../integration_config');

var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere'
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    var webhookConfig2 = {
        callbackUrl: 'http://nothing/callback2',
        channelUrl: 'http://different/channel/notHere'
    };

    utils.putWebhook(webhookName, webhookConfig2, 200, testName);

    var webhookConfig3 = {
        callbackUrl: 'http://nothing/callback2',
        channelUrl: 'http://different/channel/not_Here'
    };

    utils.putWebhook(webhookName, webhookConfig3, 409, testName);

    utils.deleteWebhook(webhookName);

});
