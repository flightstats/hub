const {
    deleteWebhook,
    getProp,
    putWebhook,
    randomChannelName,
} = require('../lib/helpers');

const webhookName = randomChannelName();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'SINGLE',
};
const webhookConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE',
};

describe(__filename, function () {
    it('create a webhook', async () => {
        const result = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', result)).toEqual(201);
    });

    it('change the webhook', async () => {
        const result = await putWebhook(webhookName, webhookConfig2, 200, __filename);
        expect(getProp('statusCode', result)).toEqual(200);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
