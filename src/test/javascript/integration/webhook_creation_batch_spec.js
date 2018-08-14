require('../integration_config');
const { getProp, putWebhook } = require('../lib/helpers');

const webhookName = utils.randomChannelName();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE',
};

const webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE',
    parallelCalls: 1,
};

describe(__filename, function () {
    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.getWebhook(webhookName, webhookConfig2);

    it('creates another webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig2, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    utils.deleteWebhook(webhookName);

    utils.getWebhook(webhookName, webhookConfig2, 404);
});
