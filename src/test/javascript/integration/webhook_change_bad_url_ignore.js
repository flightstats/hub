require('../integration_config');
const {
    createChannel,
    getProp,
    fromObjectPath,
    hubClientPostTestItem,
    itSleeps,
    putWebhook,
} = require('../lib/helpers');
const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let createdChannel = false;
let callbackServer = null;
let itemURL = null;
const port = utils.getPort();
const badConfig = {
    callbackUrl: 'http://localhost:8080/nothing',
    channelUrl: channelResource,
};
const goodConfig = {
    callbackUrl: `${callbackDomain}:${port}/`,
    channelUrl: channelResource,
};
const receivedItems = [];
/**
 * This is disabled for now.
 *
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel with a non-existent endpointA
 * 3 - re-create the webhook with the same name and a new endpointB
 * 4 - post item into the channel
 * 5 - start a server at the endpointB
 * 6 - post item - should see item at endPointB
 */
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('create a group callback webhook with bad config', async () => {
        const result = await putWebhook(webhookName, badConfig, 201, __filename);
        expect(getProp('statusCode', result)).toEqual(201);
    });

    it('waits 2000 ms', async () => {
        await itSleeps(2000);
    });

    it('create a group callback webhook with good config', async () => {
        const result = await putWebhook(webhookName, goodConfig, 200, __filename);
        expect(getProp('statusCode', result)).toEqual(200);
    });

    it('waits 10000 ms', async () => {
        await itSleeps(10000);
    });

    it('starts a callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log(`called webhook ${webhookName} ${string}`);
            receivedItems.push(string);
        }, done);
    });

    it('posts an item to the channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(receivedItems, [itemURL], done);
    });

    it('verifies we got the item through the callback', () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(receivedItems.length).toBe(1);
        const receivedItem = receivedItems.find(item => item.includes(item));
        expect(receivedItem).toBeDefined();
        expect(receivedItem).toContain(itemURL);
    });

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.closeServer(callbackServer, done);
    });
});
