require('../integration_config');

var webhookName = utils.randomChannelName();
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 2,
    batch: 'SINGLE'

};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.getWebhook(webhookName, webhookConfig);

});
