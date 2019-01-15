const moment = require('moment');
const {
    closeServer,
    deleteWebhook,
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientGetUntil,
    hubClientPost,
    hubClientPut,
    itSleeps,
    randomChannelName,
    randomString,
    startServer,
    waitForCondition,
} = require('../lib/helpers');
const {
    getCallBackDomain,
    getCallBackPort,
    getHubUrlBase,
} = require('../lib/config');

const hubUrlBase = getHubUrlBase();
const callbackDomain = getCallBackDomain();
const port = getCallBackPort();
const channelName = randomChannelName();
const channelResource = `${hubUrlBase}/channel/${channelName}`;
const errorChannelName = randomChannelName();
const errorChannelURL = `${hubUrlBase}/channel/${errorChannelName}`;
const callbackPath = `/${randomString(5)}`;
const callbackServerURL = `${callbackDomain}:${port}${callbackPath}`;
const webhookName = randomChannelName();
const webhookURL = `${hubUrlBase}/webhook/${webhookName}`;
let callbackServer;
const callbackItems = [];
const postedItems = [];
let postedTime = null;
let giveUpTime = null;

describe(__filename, () => {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a callback server', async () => {
        const callback = (str) => {
            console.log('calling callback with ', str);
            callbackItems.push(str);
        };
        callbackServer = await startServer(port, callback, callbackPath);
    });

    it('creates a webhook', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = {
            channelUrl: channelResource,
            callbackUrl: callbackServerURL,
            errorChannelUrl: errorChannelURL,
            callbackTimeoutSeconds: 10,
            maxAttempts: 1,
        };
        const response = await hubClientPut(webhookURL, headers, payload);
        console.log(getProp('body', response));
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verifies the error channel was created', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const response = await hubClientGet(errorChannelURL, headers);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('waits 1000 ms', async () => {
        await itSleeps(1000);
    });

    it('posts an item to the data channel', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = moment.utc().toISOString();
        const response = await hubClientPost(channelResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(201);
        const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(itemURL);
    });

    it('waits for the item to be delivered', async () => {
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('verifies no error was posted to the error channel', async () => {
        const latestResponse = await hubClientGet(`${errorChannelURL}/latest`);
        const redirectResponse = await followRedirectIfPresent(latestResponse);
        expect(getProp('statusCode', redirectResponse)).toEqual(404);
    });

    it('kills the callback server', async () => {
        expect(callbackServer).toBeDefined();
        await closeServer(callbackServer);
        // it takes some time for the server to actually die
        await itSleeps(10000);
    });

    it('posts an item to the data channel', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const payload = moment.utc().toISOString();
        const response = await hubClientPost(channelResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(201);
        const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(itemURL);
        postedTime = moment.utc();
        console.log('postedTime:', postedTime.toISOString());
    });

    it('waits for the webhook to give up', async () => {
        const clause = (response) => {
            const errors = fromObjectPath(['body', 'errors'], response) || [];
            console.log('response.body.errors', errors);
            return errors.some(e => e && e.includes('max attempts reached'));
        };
        try {
            await hubClientGetUntil(webhookURL, clause);
            giveUpTime = moment.utc();
            console.log('giveUpTime:', giveUpTime.toISOString());
        } catch (error) {
            return fail(error);
        };
    });

    it('verifies an error was posted to the error channel', async () => {
        expect(postedTime).toBeDefined();
        expect(giveUpTime).toBeDefined();
        const clause = res => {
            const statusCode = getProp('statusCode', res);
            console.log(statusCode);
            return (statusCode === 303);
        };
        try {
            const originalRes = await hubClientGetUntil(`${errorChannelURL}/latest`, clause);
            const headers = { 'Content-Type': 'application/json' };
            const response = await followRedirectIfPresent(originalRes, headers);
            const body = getProp('body', response) || {};
            expect(body.webhookUrl).toEqual(webhookURL);
            expect(body.failedItemUrl).toEqual(postedItems[1]);
            expect(body.callbackUrl).toEqual(callbackServerURL);
            expect(body.numberOfAttempts).toEqual(1);
            expect(moment(body.lastAttemptTime).utc()).toBeGreaterThan(postedTime);
            expect(moment(body.lastAttemptTime)).toBeLessThan(giveUpTime);
            expect(body.lastAttemptError).toEqual("max attempts reached (1)");
        } catch (error) {
            return fail(error);
        }
    });

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
