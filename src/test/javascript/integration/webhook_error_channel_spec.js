require('../integration_config');
const moment = require('moment');
const {
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPost,
    hubClientPut,
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
const dataChannelName = utils.randomChannelName();
const dataChannelURL = `${hubUrlBase}/channel/${dataChannelName}`;
const errorChannelName = utils.randomChannelName();
const errorChannelURL = `${hubUrlBase}/channel/${errorChannelName}`;
const callbackServerURL = `${callbackDomain}:${port}`;
const webhookName = utils.randomChannelName();
const webhookURL = `${hubUrlBase}/webhook/${webhookName}`;
let callbackServer;
let callbackItems = [];
let postedItems = [];
let postedTime = null;
let giveUpTime = null;

describe(__filename, () => {
    beforeAll(async () => {
        const response = await hubClientPut(dataChannelURL);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(port, (request) => {
            try {
                const json = JSON.parse(request);
                console.log('incoming:', json);
                const uris = getProp('uris', json) || [];
                callbackItems.push(...uris);
            } catch (ex) {
                return done.fail(ex);
            }
        }, done);
    });

    it('creates a webhook', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = {
            channelUrl: dataChannelURL,
            callbackUrl: callbackServerURL,
            errorChannelUrl: errorChannelURL,
            callbackTimeoutSeconds: 10,
            maxAttempts: 1,
        };
        const response = await hubClientPut(webhookURL, headers, payload);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verifies the error channel was created', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const response = await hubClientGet(`${hubUrlBase}/channel`, headers);
        const channels = fromObjectPath(['body', '_links', 'channels'], response) || [];
        expect(channels.length).toBeGreaterThan(0);
        const channelURL = channels.find(channel => getProp('href', channel) === errorChannelURL);
        expect(channelURL).toBeDefined();
    });

    it('posts an item to the data channel', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const payload = moment.utc().toISOString();
        const response = await hubClientPost(dataChannelURL, headers, payload);

        expect(getProp('statusCode', response)).toEqual(201);
        const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(itemURL);
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('verifies no error posted to the error channel', async () => {
        const response = await hubClientGet(`${errorChannelURL}/latest`);
        const response2 = await followRedirectIfPresent(response);
        expect(getProp('statusCode', response2)).toEqual(404);
    });

    it('kills the callback server', (done) => {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    it('posts an item to the data channel', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const payload = moment.utc().toISOString();
        const response = await hubClientPost(dataChannelURL, headers, payload);

        expect(getProp('statusCode', response)).toEqual(201);
        const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(itemURL);
        postedTime = moment.utc();
        console.log('postedTime:', postedTime.toISOString());
    });

    it('waits for the webhook to give up', async () => {
        const clause = (response) => {
            const errors = fromObjectPath(['body', 'errors'], response) || [];
            return errors.some(e => e && e.includes('max attempts reached'));
        };
        try {
            await utils.httpGetUntil(webhookURL, clause);
            giveUpTime = moment.utc();
            console.log('giveUpTime:', giveUpTime.toISOString());
        } catch (error) {
            return fail(error);
        }
    });

    it('verifies an error was posted to the error channel', async () => {
        expect(postedTime).toBeDefined();
        expect(giveUpTime).toBeDefined();
        const clause = res => (getProp('statusCode', res) === 303);
        try {
            const originalRes = await utils.httpGetUntil(`${errorChannelURL}/latest`, clause);
            const headers = { 'Content-Type': 'application/json' };
            const response = await followRedirectIfPresent(originalRes, headers);
            const body = getProp('body', response) || {};
            console.log('body: ', body);
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
});
