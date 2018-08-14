require('../integration_config');
const {
    createChannel,
    deleteWebhook,
    getProp,
    fromObjectPath,
    hubClientPostTestItem,
    putWebhook,
} = require('../lib/helpers');
const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const port = utils.getPort();
const callbackUrl = `${callbackDomain}:${port}/`;
const webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
};
let createdChannel = false;

/*
 * This should:
 * TODO: I do not think this is exactly what this test actually does
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post item into the channel
 * 5 - delete the webhook
 * 6 - recreate the webhook
 * 7 - post item - should only see new item
 */
describe(__filename, function () {
    let callbackServer;
    const callbackItems = [];
    const postedItems = [];
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

    utils.itSleeps(500);

    it('starts the callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            callbackItems.push(string);
        }, done);
    });

    it('posts an item to the hub', async () => {
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
        postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], response));
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });

    it('posts an item to the hub', async () => {
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.itSleeps(500);

    it('posts an item to the hub', async () => {
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
        postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], response));
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(2);
        let uriA;
        let uriB;
        try {
            const itemA = JSON.parse(callbackItems[0]);
            const itemB = JSON.parse(callbackItems[1]);
            const urisA = getProp('uris', itemA);
            const urisB = getProp('uris', itemB);
            uriA = urisA && urisA[0];
            uriB = urisB && urisB[0];
        } catch (ex) {
            expect(`failed to parse json, ${ex}`).toBeNull();
        }
        expect(uriA).toBe(postedItems[0]);
        expect(uriB).toBe(postedItems[1]);
    });

    it('closes the callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });
});
