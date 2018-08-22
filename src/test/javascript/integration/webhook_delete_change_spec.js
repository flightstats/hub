require('../integration_config');
const {
    createChannel,
    deleteWebhook,
    getProp,
    fromObjectPath,
    hubClientPostTestItem,
    itSleeps,
    putWebhook,
    waitForCondition,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let callbackServerA = null;
let callbackServerB = null;
const portA = utils.getPort();
const portB = utils.getPort();
const callbackItemsA = [];
const callbackItemsB = [];
const postedItemsA = [];
const postedItemsB = [];
const webhookConfigA = {
    callbackUrl: `${callbackDomain}:${portA}/`,
    channelUrl: channelResource,
};
const webhookConfigB = {
    callbackUrl: `${callbackDomain}:${portB}/`,
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
    });

    it('starts the first callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServerA = utils.startHttpServer(portA, function (string) {
            callbackItemsA.push(string);
        }, done);
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
    });

    it('starts the second callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServerB = utils.startHttpServer(portB, function (string) {
            callbackItemsB.push(string);
        }, done);
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
        let uriA;
        let uriB;
        try {
            const itemA = JSON.parse(callbackItemsA[0]);
            const itemB = JSON.parse(callbackItemsB[0]);
            const urisA = getProp('uris', itemA);
            const urisB = getProp('uris', itemB);
            uriA = urisA && urisA[0];
            uriB = urisB && urisB[0];
        } catch (ex) {
            expect(`failed to parse json, ${ex}`).toBeNull();
        }
        expect(uriA).toBe(postedItemsA[0]);
        expect(uriB).toBe(postedItemsB[0]);
    });

    it('closes the first callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServerA).toBeDefined();
        utils.closeServer(callbackServerA, done);
    });

    it('closes the second callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServerB).toBeDefined();
        utils.closeServer(callbackServerB, done);
    });
});
