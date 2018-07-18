require('../integration_config');

var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE'
};

var webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE',
    parallelCalls: 1
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.getWebhook(webhookName, webhookConfig2);

    utils.putWebhook(webhookName, webhookConfig2, 200, testName);

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig2, 404);
});
