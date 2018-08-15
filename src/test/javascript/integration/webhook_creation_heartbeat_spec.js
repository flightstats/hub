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
    heartbeat: false,
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
        expect(body.callbackUrl).toBe('http://nothing/callback');
        expect(body.channelUrl).toBe('http://nothing/channel/notHere');
        expect(body.name).toBe(webhookName);
        expect(body.batch).toBe('SINGLE');
        expect(body.parallelCalls).toBe(1);
        expect(body.heartbeat).toBe(false);
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
