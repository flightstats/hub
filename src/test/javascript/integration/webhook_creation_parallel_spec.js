require('../integration_config');
const {
    getProp,
    getWebhook,
    putWebhook,
} = require('../lib/helpers');

const webhookName = utils.randomChannelName();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 2,
    batch: 'SINGLE',

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
        expect(body.parallelCalls).toBe(2);
    });
});
