require('../integration_config');
const rp = require('request-promise-native');
const {
    createChannel,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    itSleeps,
    putWebhook,
} = require('../lib/helpers');
const { getChannelUrl } = require('../lib/config');

const channelName = utils.randomChannelName();
const gUrl = `${getWebhookUrl()}/${channelName}`;
const webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: `http://nothing/channel/${channelName}`,
    batch: 'SINGLE',
    parallelCalls: 1,
    paused: false,
};
let createdChannel = false;

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, getChannelUrl(), __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('creates the webhook', async () => {
        const response = await putWebhook(channelName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits 10000 ms', async () => {
        await itSleeps(10000);
    });

    it(`gets webhook ${channelName}`, async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await rp({
            method: 'GET',
            url: gUrl,
            headers: { "Content-Type": "application/json" },
            json: true,
            resolveWithFullResponse: true,
        });
        const statusCode = getProp('statusCode', response);
        expect(statusCode).toBe(200);
        const body = getProp('body', response);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], body);
        expect(selfLink).toBe(gUrl);
        const lastComp = getProp('lastCompleted', body) || '';
        console.log(`lastComp: ${lastComp}`);
        expect((lastComp || '').indexOf('initial') > -1, true);
        expect(getProp('callbackUrl', body)).toBe(webhookConfig.callbackUrl);
        expect(getProp('channelUrl', body)).toBe(webhookConfig.channelUrl);
        expect(getProp('transactional', body)).toBe(null);
        expect(getProp('name', body)).toBe(channelName);
        expect(getProp('batch', body)).toBe(webhookConfig.batch);
        // if (webhookConfig.ttlMinutes) {
        //     expect(getProp('ttlMinutes', body)).toBe(webhookConfig.ttlMinutes);
        // }
        // if (webhookConfig.maxWaitMinutes) {
        //     expect(getProp('maxWaitMinutes', body)).toBe(webhookConfig.maxWaitMinutes);
        // }
    });
});
