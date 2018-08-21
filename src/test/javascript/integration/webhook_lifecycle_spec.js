require('../integration_config');
const request = require('request');
const {
    closeServer,
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    hubClientPostTestItem,
    putWebhook,
    randomString,
    startServer,
    waitForCondition,
} = require('../lib/helpers');
const {
    getCallBackDomain,
    getCallBackPort,
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const callbackDomain = getCallBackDomain();
const port = getCallBackPort();
const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const callbackPath = `/${randomString(5)}`;
const callbackUrl = `${callbackDomain}:${port}${callbackPath}`;
const webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
};
let createdChannel = false;
let callbackServer = null;
const callbackItems = [];
const postedItems = [];

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 */
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('starts a callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const callback = (string) => {
            console.log('incoming:', string);
            const json = JSON.parse(string);
            const uris = json.uris || [];
            uris.forEach(uri => callbackItems.push(uri));
        };
        callbackServer = await startServer(port, callback, callbackPath);
    });

    it('inserts items', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response0 = await hubClientPostTestItem(channelResource);
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        const items = [response0, response1, response2, response3]
            .map(value => fromObjectPath(['body', '_links', 'self', 'href'], value));
        postedItems.push(...items);
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toEqual(postedItems.length);
        for (let i = 0; i < callbackItems.length; i++) {
            expect(callbackItems[i]).toEqual(postedItems[i]);
        }
    });

    it('verifies lastCompleted', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        const webhookResource = `${getWebhookUrl()}/${webhookName}`;
        request.get({
            url: webhookResource,
            headers: { "Content-Type": "application/json" } },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            const parse = utils.parseJson(response, __filename);
            const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
            expect(selfLink).toBe(webhookResource);
            if (typeof webhookConfig !== "undefined") {
                expect(getProp('callbackUrl', parse)).toBe(webhookConfig.callbackUrl);
                expect(getProp('channelUrl', parse)).toBe(webhookConfig.channelUrl);
                // expect(getProp('transactional', parse)).toBe(webhookConfig.transactional);
                expect(getProp('name', parse)).toBe(webhookName);
                expect(getProp('lastCompleted', parse)).toBe(postedItems[3]);
            }
            done();
        });
    });

    it('closes the callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        await closeServer(callbackServer);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
