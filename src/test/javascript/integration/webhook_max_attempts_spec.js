require('../integration_config');
const moment = require('moment');
const {
    closeServer,
    deleteWebhook,
    fromObjectPath,
    getProp,
    getWebhookUrl,
    hubClientGet,
    hubClientGetUntil,
    hubClientPost,
    hubClientPut,
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
const callbackDomain = getCallBackDomain();
const port = getCallBackPort();
const webhookName = utils.randomChannelName();
const callbackPath = `/${randomString(5)}`;
const callbackServerURL = `${callbackDomain}:${port}${callbackPath}`;
const postedItems = [];
const callbackItems = [];
const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
const webhookResource = `${getWebhookUrl()}/${webhookName}`;
const headers = { 'Content-Type': 'application/json' };
let callbackServer = null;

describe(__filename, () => {
    it('creates a channel', async () => {
        const response = await hubClientPut(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a callback server', async () => {
        const callback = (str, response) => {
            callbackItems.push(str);
            console.log('callbackItems', callbackItems);
            response.statusCode = 400;
        };
        callbackServer = await startServer(port, callback, callbackPath);
    });

    it('creates a webhook', async () => {
        const payload = {
            channelUrl: channelResource,
            callbackUrl: callbackServerURL,
            callbackTimeoutSeconds: 1,
        };
        const response = await hubClientPut(webhookResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verify default max attempts is 0', async () => {
        const response = await hubClientGet(webhookResource, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const maxAttempts = fromObjectPath(['body', 'maxAttempts'], response);
        console.log('maxAttempts:', maxAttempts);
        expect(maxAttempts).toEqual(0);
    });

    it('updates the max attempts to 1', async () => {
        const payload = { maxAttempts: 1 };
        const response = await hubClientPut(webhookResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(200);
        const maxAttempts = fromObjectPath(['body', 'maxAttempts'], response);
        console.log('maxAttempts:', maxAttempts);
        expect(maxAttempts).toEqual(1);
    });

    it('posts an item to our channel', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const payload = moment().utc().toISOString();
        const response = await hubClientPost(channelResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(201);
        const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(itemURL);
        console.log('itemURL:', itemURL);
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('waits for the webhook to give up', async () => {
        const timeoutMS = 5 * 1000;
        const getUntilCallback = (response) => {
            const errorsArray = fromObjectPath(['body', 'errors'], response) || [];
            return errorsArray.some(err => (err || '').includes('max attempts reached'));
        };
        const response = await hubClientGetUntil(webhookResource, getUntilCallback, timeoutMS);
        const errors = fromObjectPath(['body', 'errors'], response) || [];
        expect(errors.length).toBeGreaterThan(0);
    });

    it('verifies we received the item only once', () => {
        console.log('callbackItems:', callbackItems);
        expect(callbackItems.length).toEqual(1);
    });

    it('verifies the webhook gave up after 1 attempt', async () => {
        const response = await hubClientGet(webhookResource, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const body = getProp('body', response) || {};
        const {
            channelLatest,
            errors = [],
            inFlight = [],
            lastCompleted,
        } = body;
        expect(lastCompleted).toEqual(channelLatest);
        expect(body.inFlight).toBeDefined();
        expect(inFlight.length).toEqual(0);
        expect(errors.length).toEqual(2);
        let contentKey = (postedItems[0] || '').replace(`${channelResource}/`, '');
        expect(errors[0]).toContain(contentKey);
        expect(errors[0]).toContain('400 Bad Request');
        expect(errors[1]).toContain(`${contentKey} max attempts reached (1)`);
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
