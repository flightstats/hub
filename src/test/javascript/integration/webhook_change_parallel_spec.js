require('../integration_config');
const { getProp, putWebhook } = require('../lib/helpers');

const webhookName = utils.randomChannelName();
const webhookConfigA = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 1,
    batch: 'SINGLE',
};

const webhookConfigB = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 2,
    batch: 'SINGLE',
};

describe(__filename, function () {
    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfigA, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.getWebhook(webhookName, webhookConfigA);

    it('creates another webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfigB, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    utils.getWebhook(webhookName, webhookConfigB);
});
