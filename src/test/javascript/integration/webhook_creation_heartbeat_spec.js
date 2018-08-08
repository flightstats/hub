require('../integration_config');

const webhookName = utils.randomChannelName();
const testName = __filename;
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false
};

const webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false,
    batch: 'SINGLE',
    parallelCalls: 1
};

describe(testName, function () {

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.getWebhook(webhookName, webhookConfig2);

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig2, 404);
});
