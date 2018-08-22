require('../integration_config');
const {
    closeServer,
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    hubClientGet,
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
    getHubUrlBase,
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
        isClustered: false,
    },
};

describe('callback leak on same callbackServer, port, path', () => {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            console.log(`created channel for ${__filename}`);
        }

        const headers = { 'Content-Type': 'application/json' };
        const url = `${getHubUrlBase()}/internal/properties`;
        const response1 = await hubClientGet(url, headers);
        const properties = fromObjectPath(['body', 'properties'], response1) || {};
        const hubType = properties['hub.type'];
        context[channelUrl].isClustered = hubType === 'aws';
        console.log('isClustered:', context[channelUrl].isClustered);
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
            console.log('called second webhook ', webhookName0, string);
            context[channelUrl].callbackItemsB.push(`SECOND_SET: ${string}`);
        };
        context[channelUrl].callbackServer2 = await startServer(port, callback);
    });

    it('inserts items', async () => {
        await hubClientPostTestItem(channelResourceB, headers);
        await hubClientPostTestItem(channelResourceB, headers);
        await hubClientPostTestItem(channelResourceB, headers);
        await hubClientPostTestItem(channelResourceB, headers);
        let condition = () => (context[channelUrl].callbackItemsA.length === 8);
        if (context[channelUrl].isClustered) {
            condition = () => (context[channelUrl].callbackItemsB.length === 4);
        }
        await waitForCondition(condition);
    });

    it('leaks callback items from the second callback into the first', () => {
        let actual = false;
        if (context[channelUrl].isClustered) {
            expect(context[channelUrl].callbackItemsA.length).toEqual(4);
            expect(context[channelUrl].callbackItemsB.length).toEqual(4);
            actual = context[channelUrl].callbackItemsA.every(item => item && item.includes('FIRST_SET'));
            actual = actual && context[channelUrl].callbackItemsB.every(item =>
                item && item.includes('SECOND_SET'));
        } else {
            expect(context[channelUrl].callbackItemsA.length).toEqual(8);
            expect(context[channelUrl].callbackItemsB.length).toEqual(0);
            actual = context[channelUrl].callbackItemsA.every(item => item && item.includes('FIRST_SET'));
        }
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
