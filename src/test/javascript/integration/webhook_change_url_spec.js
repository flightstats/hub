require('../integration_config');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientPost,
    hubClientPut,
} = require('../lib/helpers');

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel with a non-existent endpointA
 * 3 - post item into the channel
 * 4 - change the webhook with the same name and a new endpointB
 * 5 - start a server at the endpointB
 * 6 - post item - should see items at endPointB
 */

const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
const webhookName = utils.randomChannelName();
const webhookResource = `${utils.getWebhookUrl()}/${webhookName}`;

describe(__filename, () => {
    let callbackServer = null;
    const callbackServerPort = utils.getPort();
    const callbackServerURL = `${callbackDomain}:${callbackServerPort}/${webhookName}`;

    const postedItems = [];
    const callbackItems = [];

    it('creates a channel', async () => {
        const response = await hubClientPut(channelResource);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('creates a webhook', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = {
            channelUrl: channelResource,
            callbackUrl: 'http://nothing:8080/nothing',
        };
        const response = await hubClientPut(webhookResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(201);
        console.log('callbackURL:', fromObjectPath(['body', 'callbackUrl'], response));
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

    it('verifies the correct delivery error was logged', (done) => {
        let timeoutMS = 10 * 1000;
        utils.httpGetUntil(webhookResource, (response) => response.body.errors.length > 0, timeoutMS)
            .then(response => {
                const body = getProp('body', response) || {};
                console.log(body);
                const {
                    lastCompleted,
                    inFlight = [],
                    errors = [],
                } = body;
                expect(lastCompleted).toContain('initial');
                expect(inFlight.length).toEqual(1);
                expect(inFlight[0]).toEqual(postedItems[0]);
                expect(errors.length).toEqual(1);
                expect(errors[0]).toContain('java.net.UnknownHostException');
            })
            .finally(done);
    });

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackServerPort, (request) => {
            const json = JSON.parse(request);
            console.log('incoming:', json);
            const uris = getProp('uris', json) || [];
            callbackItems.push(...uris);
        }, done);
    });

    it('updates the webhook\'s callbackURL', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = {
            channelUrl: channelResource,
            callbackUrl: callbackServerURL,
        };
        const response = await hubClientPut(webhookResource, headers, payload);
        expect(getProp('statusCode', response)).toEqual(200);
        console.log('callbackURL:', fromObjectPath(['body', 'callbackUrl'], response));
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

    it('verifies the webhook delivered both items', () => {
        expect(callbackItems.length).toEqual(postedItems.length);
        expect(callbackItems[0]).toEqual(postedItems[0]);
        expect(callbackItems[1]).toEqual(postedItems[1]);
    });

    it('closes the callback server', (done) => {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });
});
