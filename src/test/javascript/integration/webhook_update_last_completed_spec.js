require('../integration_config');
const moment = require('moment');
const {
    closeServer,
    deleteWebhook,
    getProp,
    fromObjectPath,
    hubClientGet,
    hubClientPost,
    hubClientPut,
    itSleeps,
    putWebhook,
    randomString,
    startServer,
} = require('../lib/helpers');
const {
    getCallBackDomain,
    getCallBackPort,
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const callbackDomain = getCallBackDomain();
const port = getCallBackPort();
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const webhookName = utils.randomChannelName();
const webhookURL = `${getHubUrlBase()}/webhook/${webhookName}`;
const callbackPath = `/${randomString(5)}`;
const contentTypeJSON = { 'Content-Type': 'application/json' };
const contentTypePlain = { 'Content-Type': 'text/plain' };

let callbackServer;
const callbackMessages = [];
const postedItems = [];

let maxCursor = null;
let minCursor = null;
let firstItemURL = null;
const webhookConfig = {
    callbackUrl: `${callbackDomain}:${port}${callbackPath}`,
    channelUrl: channelResource,
    parallelCalls: 1,
    batch: 'SECOND',
    heartbeat: true,
};

describe(__filename, () => {
    it('runs a callback server', async () => {
        const callback = (message) => {
            callbackMessages.push(message);
        };
        callbackServer = await startServer(port, callback, callbackPath);
    });

    it('creates a channel', async () => {
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, contentTypeJSON, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, webhookURL);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('inserts the first item', async () => {
        const response = await hubClientPost(channelResource, contentTypePlain, "a test " + Date.now());
        expect(getProp('statusCode', response)).toEqual(201);
        firstItemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(firstItemURL);
    });

    it('waits 5000 ms', async () => {
        await itSleeps(5000);
    });

    // Check the latest on the webhook
    it('sets maxItem', async () => {
        const response = await hubClientGet(webhookURL, contentTypeJSON);
        maxCursor = fromObjectPath(['body', 'lastCompleted'], response);
    });

    it('moves the cursor backward', async () => {
        const y = moment().subtract(1, 'day');
        const formatted = y.format("YYYY/MM/DD/HH/mm/ss");
        const url = channelResource + "/" + formatted;
        console.log("backward cursor ", url);
        const response = await hubClientPut(webhookURL + "/updateCursor", contentTypePlain, url);
        expect(getProp('statusCode', response)).toBeLessThan(300);
    });

    it('waits 5000 ms', async () => {
        await itSleeps(5000);
    });

    it('checks to see if latest is before "maxCursor"', async () => {
        const response = await hubClientGet(webhookURL, contentTypeJSON, false);
        const lastCompleted = fromObjectPath(['body', 'lastCompleted'], response);
        expect(lastCompleted).toBeLessThan(maxCursor);
        minCursor = lastCompleted;
    });

    it('moves the cursor forward', async () => {
        const y = moment().subtract(1, 'hour');
        const formatted = y.format("YYYY/MM/DD/HH/mm/ss");
        const url = channelResource + "/" + formatted;
        console.log("forward cursor ", url);
        // const item = { 'item': url };
        const response = await hubClientPut(webhookURL + "/updateCursor", contentTypePlain, url);
        expect(getProp('statusCode', response)).toBeLessThan(300);
    });

    it('waits 5000 ms', async () => {
        await itSleeps(5000);
    });

    it('checks to see if latest is after "minCursor"', async () => {
        const response = await hubClientGet(webhookURL, contentTypeJSON, false);
        const lastCompleted = fromObjectPath(['body', 'lastCompleted'], response);
        expect(lastCompleted).toBeGreaterThan(minCursor);
        minCursor = lastCompleted;
    });

    it('closes the callback server', async () => {
        expect(callbackServer).toBeDefined();
        await closeServer(callbackServer);
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
