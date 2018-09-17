const {
    closeServer,
    deleteWebhook,
    createChannel,
    fromObjectPath,
    getProp,
    hubClientPostTestItem,
    hubClientDelete,
    itSleeps,
    putWebhook,
    randomChannelName,
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
const channelName = randomChannelName();
const webhookName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const callbackPath = `/${randomString(5)}`;
const callbackUrl = `${callbackDomain}:${port}${callbackPath}`;
const webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
};
const callbackItems = [];
const postedItems = [];
let callbackServer = null;
let createdChannel = false;

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

    it('posts 2 items to the channel', async () => {
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response1)).toEqual(201);
        expect(getProp('statusCode', response2)).toEqual(201);
    });

    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('runs callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const callback = (string) => {
            console.log(`called webhook ${webhookName} ${string}`);
            callbackItems.push(string);
        };
        callbackServer = await startServer(port, callback, callbackPath);
    });

    it('posts 4 items to the channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        const response4 = await hubClientPostTestItem(channelResource);
        const responses = [response1, response2, response3, response4];
        const successes = responses.every(r => getProp('statusCode', r) === 201);
        expect(successes).toEqual(true);
        const items = responses
            .map(res => fromObjectPath(['body', '_links', 'self', 'href'], res));
        postedItems.push(...items);
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        const actual = callbackItems.every((callbackItem, index) => {
            return callbackItem === postedItems[index];
        });
        expect(actual).toBe(true);
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

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
