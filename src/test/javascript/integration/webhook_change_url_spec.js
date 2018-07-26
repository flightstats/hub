require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientPost,
} = require('../lib/helpers');
const moment = require('moment');

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

    it('creates a channel', (done) => {
        utils.httpPut(channelResource)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    it('creates a webhook', (done) => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = {
            channelUrl: channelResource,
            callbackUrl: 'http://nothing:8080/nothing',
        };
        utils.httpPut(webhookResource, headers, payload)
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                console.log('callbackURL:', fromObjectPath(['body', 'callbackUrl'], response));
            })
            .finally(done);
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

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackServerPort, (request) => {
            const json = JSON.parse(request);
            console.log('incoming:', json);
            const uris = getProp('uris', json) || [];
            uris.forEach(uri => callbackItems.push(uri));
        }, done);
    });

    it('updates the webhook\'s callbackURL', (done) => {
        const headers = { 'Content-Type': 'application/json' };
        const payload = {
            channelUrl: channelResource,
            callbackUrl: callbackServerURL,
        };
        utils.httpPut(webhookResource, headers, payload)
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(200);
                console.log('callbackURL:', fromObjectPath(['body', 'callbackUrl'], response));
            })
            .finally(done);
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

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });
});
