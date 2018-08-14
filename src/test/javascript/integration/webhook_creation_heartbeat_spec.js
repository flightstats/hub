require('../integration_config');
const { getProp, putWebhook } = require('../lib/helpers');

const webhookName = utils.randomChannelName();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false,
};

const webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false,
    batch: 'SINGLE',
    parallelCalls: 1,
};

describe(__filename, function () {
    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.getWebhook(webhookName, webhookConfig2);

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig2, 404);
});
