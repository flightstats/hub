const {
    closeServer,
    createChannel,
    deleteWebhook,
    getProp,
    fromObjectPath,
    hubClientDelete,
    hubClientPostTestItem,
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
const port = getCallBackPort();
const callbackDomain = getCallBackDomain();
const callbackPath = `/${randomString(5)}`;
const channelName = randomChannelName();
const webhookName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let callbackServerA = null;
let callbackServerB = null;
const portA = port + 1;
const portB = portA + 1;
const callbackItemsA = [];
const callbackItemsB = [];
const postedItemsA = [];
const postedItemsB = [];
console.log('portA', portA);
console.log('portB', portB);
const webhookConfigA = {
    callbackUrl: `${callbackDomain}:${portA}${callbackPath}`,
    channelUrl: channelResource,
};
const webhookConfigB = {
    callbackUrl: `${callbackDomain}:${portB}${callbackPath}`,
    channelUrl: channelResource,
};
let createdChannel = false;
/*
 * This should:
 *
 * 1 - create a channel
 * 2 - create webhook on that channel at endpointA
 * 3 - start a server at endpointA
 * 4 - post item into the channel
 * 5 - delete the webhook
 * 6 - create the webhook with the same name and a different endpoint
 * 7 - start a server at endpointB
 * 8 - post item - should see item on endpointB
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
        const response = await putWebhook(webhookName, webhookConfigA, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    }, 2 * 60 * 1000);

    it('starts the first callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const callback = (string) => {
            callbackItemsA.push(string);
        };
        callbackServerA = await startServer(portA, callback, callbackPath);
    });

    it('posts the first item', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        postedItemsA.push(fromObjectPath(['body', '_links', 'self', 'href'], response));
        const condition = () => (postedItemsA.length &&
            (postedItemsA.length === callbackItemsA.length));
        await waitForCondition(condition);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });

    it('waits 5000 ms', async () => {
        await itSleeps(5000);
    });

    it('recreates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfigB, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    }, 2 * 60 * 1000);

    it('starts the second callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const callback = (string) => {
            callbackItemsB.push(string);
        };
        callbackServerB = await startServer(portB, callback, callbackPath);
    });

    it('posts the second item', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        postedItemsB.push(fromObjectPath(['body', '_links', 'self', 'href'], response));
        const condition = () => (postedItemsB.length &&
            (postedItemsB.length === callbackItemsB.length));
        await waitForCondition(condition);
    });

    it('verifies we got what we expected through the callback', () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItemsA.length).toBe(1);
        expect(callbackItemsB.length).toBe(1);
        expect(callbackItemsA[0]).toBe(postedItemsA[0]);
        expect(callbackItemsB[0]).toBe(postedItemsB[0]);
    });

    it('closes the first callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackServerA).toBeDefined();
        await closeServer(callbackServerA);
    });

    it('closes the second callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackServerB).toBeDefined();
        await closeServer(callbackServerB);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
