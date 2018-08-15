require('../integration_config');
const { createChannel,
    fromObjectPath,
    getProp,
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
    utils.itSleeps(1000);

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

    let callbackServer;
    const callbackItems = [];
    const postedItems = [];

    it('runs callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            callbackItems.push(string);
        }, done);
    });

    it('posts 4 items to the channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        const response4 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response1)).toEqual(201);
        expect(getProp('statusCode', response2)).toEqual(201);
        expect(getProp('statusCode', response3)).toEqual(201);
        expect(getProp('statusCode', response4)).toEqual(201);
        const items = [response1, response2, response3, response4]
            .map(res => fromObjectPath(['body', '_links', 'self', 'href'], res));
        postedItems.push(...items);
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('closes the callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        const actual = callbackItems.every((callbackItem, index) => {
            try {
                const parse = JSON.parse(callbackItem);
                const uris = getProp('uris', parse) || [];
                const name = getProp('name', parse);
                return uris[0] === postedItems[index] &&
                    name === webhookName;
            } catch (ex) {
                console.log(`failed to parse json, ${callbackItem}, ${ex}`);
            }
        });
        expect(actual).toBe(true);
    });
});
