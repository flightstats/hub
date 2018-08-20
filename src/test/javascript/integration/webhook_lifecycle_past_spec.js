require('../integration_config');
const {
    closeServer,
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    hubClientPut,
    hubClientPostTestItem,
    itSleeps,
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
const callbackUrl = `${callbackDomain}:${port}/`;
let createdChannel = false;
const postedItems = [];
let firstItem = null;
const addPostedItem = (value) => {
    postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
    console.log('postedItems', postedItems);
};
let callbackServer;
const callbackItems = [];

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - add items to the channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the item are returned within delta time, excluding items posted in 2.
 */
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it(`posts initial items ${channelResource}`, async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        firstItem = fromObjectPath(['body', '_links', 'self', 'href'], response);
        const response1 = await hubClientPostTestItem(channelResource);
        addPostedItem(response1);
    });

    it('creates a webhook', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const url = `${getWebhookUrl()}/${webhookName}`;
        const headers = { 'Content-Type': 'application/json' };
        const body = {
            callbackUrl,
            channelUrl: channelResource,
            startItem: firstItem,
        };

        const response = await hubClientPut(url, headers, body);
        const responseBody = getProp('body', response) || {};
        const location = fromObjectPath(['headers', 'location'], response);
        expect(getProp('statusCode', response)).toBe(201);
        expect(location).toBe(url);
        expect(responseBody.callbackUrl).toBe(callbackUrl);
        expect(responseBody.channelUrl).toBe(channelResource);
        expect(responseBody.name).toBe(webhookName);
    });

    it('starts a callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const callback = (string) => {
            console.log('called webhook ', webhookName, string);
            callbackItems.push(string);
        };
        callbackServer = await startServer(port, callback);
    });

    it('inserts items', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response0 = await hubClientPostTestItem(channelResource);
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        [response0, response1, response2, response3]
            .forEach(res => addPostedItem(res));
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(5);
        expect(postedItems.length).toBe(5);
        for (var i = 0; i < callbackItems.length; i++) {
            let parse = {};
            try {
                parse = JSON.parse(callbackItems[i]);
            } catch (ex) {
                expect(`failed to parse json, ${callbackItems[i]}, ${ex}`).toBeNull();
            }
            const uris = getProp('uris', parse) || [];
            const name = getProp('name', parse);
            expect(uris[0]).toBe(postedItems[i]);
            expect(name).toBe(webhookName);
        }
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
