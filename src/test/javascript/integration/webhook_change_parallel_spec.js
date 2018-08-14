require('../integration_config');
const {
    getProp,
    getWebhook,
    putWebhook,
} = require('../lib/helpers');

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

    it('verifies the webhook', async () => {
        const response = await getWebhook(webhookName, webhookConfigA);
        expect(getProp('statusCode', response)).toEqual(200);
        const body = getProp('body', response) || {};
        expect(body.callbackUrl).toBe(webhookConfigA.callbackUrl);
        expect(body.channelUrl).toBe(webhookConfigA.channelUrl);
        expect(body.transactional).toBe(webhookConfigA.transactional);
        expect(body.name).toBe(webhookName);
        expect(body.batch).toBe(webhookConfigA.batch);
    });

    it('creates another webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfigB, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('verifies the webhook', async () => {
        const response = await getWebhook(webhookName, webhookConfigB);
        expect(getProp('statusCode', response)).toEqual(200);
        const body = getProp('body', response) || {};
        expect(body.callbackUrl).toBe(webhookConfigB.callbackUrl);
        expect(body.channelUrl).toBe(webhookConfigB.channelUrl);
        expect(body.transactional).toBe(webhookConfigB.transactional);
        expect(body.name).toBe(webhookName);
        expect(body.batch).toBe(webhookConfigB.batch);
    });
});
