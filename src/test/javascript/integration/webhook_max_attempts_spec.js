require('../integration_config');
const {
    fromObjectPath,
    getProp,
    getWebhookUrl,
    hubClientGet,
    hubClientPost,
    hubClientPut,
} = require('../lib/helpers');
const moment = require('moment');

const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
const webhookName = utils.randomChannelName();
const webhookResource = `${getWebhookUrl()}/${webhookName}`;
const headers = { 'Content-Type': 'application/json' };

describe(__filename, () => {
    let callbackServer;
    const callbackServerPort = utils.getPort();
    const callbackServerURL = `${callbackDomain}:${callbackServerPort}/${webhookName}`;
    const postedItems = [];
    const callbackItems = [];

    it('creates a channel', async () => {
        const response = await hubClientPut(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackServerPort, (request, response) => {
            let json = {};
            try {
                json = JSON.parse(request) || {};
                console.log('callback server received item:', json);
            } catch (ex) {
                console.log(`error parsing json: ${ex}`);
            }
            const uris = getProp('uris', json) || [];
            callbackItems.push(...uris);
            console.log('callbackItems', callbackItems);
            response.statusCode = 400;
        }, done);
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
    });

    it('waits for the callback server to receive the data', (done) => {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('waits for the webhook to give up', (done) => {
        let timeoutMS = 5 * 1000;
        const getUntilCallback = (response) => {
            const errorsArray = fromObjectPath(['body', 'errors'], response) || [];
            return errorsArray.some(err => (err || '').includes('max attempts reached'));
        };
        utils.httpGetUntil(
            webhookResource,
            getUntilCallback,
            timeoutMS
        ).finally(done);
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

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });
});
