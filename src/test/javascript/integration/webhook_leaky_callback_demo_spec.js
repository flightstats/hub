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
    randomChannelName,
    startServer,
    waitForCondition,
} = require('../lib/helpers');
const {
    getCallBackDomain,
    getCallBackPort,
    getChannelUrl,
} = require('../lib/config');

const headers = { 'Content-Type': 'application/json' };
const channelUrl = getChannelUrl();
const callbackDomain = getCallBackDomain();
const port = getCallBackPort();
const channelName = randomChannelName();
const webhookName = randomChannelName();
const channelName0 = randomChannelName();
const webhookName0 = randomChannelName();
const channelResourceA = `${channelUrl}/${channelName}`;
const channelResourceB = `${channelUrl}/${channelName0}`;
const callbackUrl = `${callbackDomain}:${port}`;
const context = {
    [channelUrl]: {
        callbackServer1: null,
        callbackServer2: null,
        callbackItemsA: [],
        callbackItemsB: [],
    },
};

describe('callback leak on same callbackServer, port, path', () => {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            console.log(`created channel for ${__filename}`);
        }
    });

    it('creates a webhook', async () => {
        const url = `${getWebhookUrl()}/${webhookName}`;
        const body = {
            callbackUrl,
            channelUrl: channelResourceA,
        };
        const response = await hubClientPut(url, headers, body);
        expect(getProp('statusCode', response)).toBe(201);
    });

    it('starts a callback server', async () => {
        const callback = (string) => {
            console.log('called webhook ', webhookName);
            context[channelUrl].callbackItemsA.push(`FIRST_SET: ${string}`);
        };
        context[channelUrl].callbackServer1 = await startServer(port, callback);
    });

    it('inserts items', async () => {
        const response0 = await hubClientPostTestItem(channelResourceA, headers);
        const response1 = await hubClientPostTestItem(channelResourceA, headers);
        const response2 = await hubClientPostTestItem(channelResourceA, headers);
        const response3 = await hubClientPostTestItem(channelResourceA, headers);
        const items = [response0, response1, response2, response3]
            .map(res => `FIRST_SET: ${fromObjectPath(['body', '_links', 'self', 'href'], res)}`);
        const condition = () => (context[channelUrl].callbackItemsA.length === items.length);
        await waitForCondition(condition);
    });

    it('closes the callback server', async () => {
        expect(context[channelUrl].callbackServer1).toBeDefined();
        await closeServer(context[channelUrl].callbackServer1);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });

    beforeAll(async () => {
        const channel = await createChannel(channelName0, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            console.log(`created channel for ${__filename}`);
        }
    });

    it('creates a webhook', async () => {
        const url = `${getWebhookUrl()}/${webhookName0}`;
        const body = {
            callbackUrl,
            channelUrl: channelResourceB,
        };
        const response = await hubClientPut(url, headers, body);
        expect(getProp('statusCode', response)).toBe(201);
    });

    it('starts a callback server', async () => {
        const callback = (string) => {
            console.log('called webhook ', webhookName0, string);
            context[channelUrl].callbackItemsB.push(`SECOND_SET: ${string}`);
        };
        context[channelUrl].callbackServer2 = await startServer(port, callback);
    });

    it('inserts items', async () => {
        await hubClientPostTestItem(channelResourceB, headers);
        await hubClientPostTestItem(channelResourceB, headers);
        await hubClientPostTestItem(channelResourceB, headers);
        await hubClientPostTestItem(channelResourceB, headers);
        const condition = () => (context[channelUrl].callbackItemsA.length === 8);
        await waitForCondition(condition);
    });

    it('leaks callback items from the first callback into the second', () => {
        expect(context[channelUrl].callbackItemsA.length).toEqual(8);
        expect(context[channelUrl].callbackItemsB.length).toEqual(0);
        const actual = context[channelUrl].callbackItemsA.every(item => item.includes('FIRST_SET'));
        expect(actual).toBe(true);
    });

    it('closes the callback server', async () => {
        expect(context[channelUrl].callbackServer2).toBeDefined();
        await closeServer(context[channelUrl].callbackServer2);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName0);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
