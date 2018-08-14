require('../integration_config');
const { getProp, putWebhook } = require('../lib/helpers');

const webhookName = utils.randomChannelName();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
};

const webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 1,
    batch: 'SINGLE',
};

describe(__filename, function () {
    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.getWebhook(webhookName, webhookConfig2);

    it('updates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig2, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig2, 404);
});
