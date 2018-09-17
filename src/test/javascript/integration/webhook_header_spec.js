const {
    closeServer,
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientDelete,
    hubClientPostTestItem,
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
    getHubUrlBase
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
let createdChannel = false;
let callbackServer = null;
const requests = [];

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - start a server at the endpoint
 * 3 - create a webhook on that channel
 * 4 - post items into the channel
 * 5 - verify the headers on the delivered item
 */

describe(__filename, function () {

    beforeAll(async () => {
        const channel = await createChannel(channelName, false);
        createdChannel = getProp('statusCode', channel) === 201;
    });

    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('starts a callback server', async () => {
        const callback = (string, response, request) => {
            console.log(`item delivered: ${string}`);
            requests.push(request);
        };
        callbackServer = await startServer(port, callback, callbackPath);
    });

    it('inserts item', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
        const condition = () => (requests.length > 0);
        await waitForCondition(condition);
    });

    it('verifies the headers on the delivered item', async () => {
        if (requests.length < 1) return fail('no requests to verify');
        const deployResponse = await hubClientGet(`${getHubUrlBase()}/internal/deploy`);
        let nodes = getProp('body', deployResponse) || [];
        expect(requests.length).toEqual(1);
        const node = fromObjectPath(['headers', 'hub-node'], requests[0]) || "";
        expect(nodes).toContain(node);
    });

    it('closes the callback server', async () => {
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
