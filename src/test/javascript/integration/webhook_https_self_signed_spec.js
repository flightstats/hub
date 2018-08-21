require('../integration_config');
const {
    closeServer,
    deleteWebhook,
    createChannel,
    getProp,
    fromObjectPath,
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

const callbackDomain = getCallBackDomain();
const port = getCallBackPort();
const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const callbackPath = `/${randomString(5)}`;
const channelResource = `${getChannelUrl()}/${channelName}`;
const callbackUrl = `${callbackDomain}:${port}${callbackPath}`.replace('http', 'https');
const webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
};
let createdChannel = false;
const callbackItems = [];
const postedItems = [];
let callbackServer = null;

/*
    TODO: this test will fail ~50% of the time
    if there are failing webhooks from other tests
    including if those tests test failover behavior of
    failing webhooks
    either a test bug or a bug bug ? investigate/fix
*/
/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the https endpoint
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

    it('runs callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const callback = (string) => {
            callbackItems.push(string);
        };
        callbackServer = await startServer(port, callback, callbackPath, true);
    });

    it('posts 4 items to the channel', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response0 = await hubClientPostTestItem(channelResource);
        const response1 = await hubClientPostTestItem(channelResource);
        const response2 = await hubClientPostTestItem(channelResource);
        const response3 = await hubClientPostTestItem(channelResource);
        const items = [response0, response1, response2, response3]
            .map(res => fromObjectPath(['body', '_links', 'self', 'href'], res));
        postedItems.push(...items);
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('closes the callback server', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        await closeServer(callbackServer);
    });

    it('verifies we got what we expected through the callback', () => {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        console.log('callbackItems', callbackItems);
        console.log('postedItems', postedItems);
        const actual = callbackItems.every((callbackItem, index) => callbackItem === postedItems[index]);
        expect(actual).toBe(true);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
