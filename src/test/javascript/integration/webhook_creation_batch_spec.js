require('../integration_config');
const {
    deleteWebhook,
    getProp,
    getWebhook,
    putWebhook,
} = require('../lib/helpers');

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
    parallelCalls: 2,
};

describe(__filename, function () {
    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verifies the webhook', async () => {
        const response = await getWebhook(webhookName);
        expect(getProp('statusCode', response)).toEqual(200);
        const body = getProp('body', response) || {};
        expect(body.callbackUrl).toBe(webhookConfig.callbackUrl);
        expect(body.channelUrl).toBe(webhookConfig.channelUrl);
        expect(body.name).toBe(webhookName);
        expect(body.batch).toBe(webhookConfig.batch);
        expect(body.parallelCalls).toBe(1);
    });

    it('updates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig2, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
        const body = getProp('body', response) || {};
        expect(body.parallelCalls).toBe(2);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });

    it('verifies the deletion of the webhook', async () => {
        const response = await getWebhook(webhookName);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
