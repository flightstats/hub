require('../integration_config');
const {
    deleteWebhook,
    getProp,
    putWebhook,
} = require('../lib/helpers');

const webhookName = utils.randomChannelName();
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
};
const webhookConfig2 = {
    callbackUrl: 'http://nothing/callback2',
    channelUrl: 'http://different/channel/notHere',
};
const webhookConfig3 = {
    callbackUrl: 'http://nothing/callback2',
    channelUrl: 'http://different/channel/not_Here',
};

describe(__filename, function () {
    it('create a webhook', async () => {
        const result = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', result)).toEqual(201);
    });

    it('change a webhook', async () => {
        const result = await putWebhook(webhookName, webhookConfig2, 200, __filename);
        expect(getProp('statusCode', result)).toEqual(200);
    });

    it('fail change a webhook', async () => {
        const result = await putWebhook(webhookName, webhookConfig3, 409, __filename);
        expect(getProp('statusCode', result)).toEqual(409);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
