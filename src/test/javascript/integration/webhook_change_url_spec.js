require('../integration_config');
const {
    fromObjectPath,
    getProp,
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

    let callbackServer;
    let callbackServerPort = utils.getPort();
    let callbackServerURL = `${callbackDomain}:${callbackServerPort}/${webhookName}`;

    let postedItems = [];
    let callbackItems = [];

    it('creates a channel', (done) => {
        utils.httpPut(channelResource)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    it('creates a webhook', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {
            channelUrl: channelResource,
            callbackUrl: 'http://nothing:8080/nothing'
        };
        utils.httpPut(webhookResource, headers, payload)
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                console.log('callbackURL:', fromObjectPath(['body', 'callbackUrl'], response));
            })
            .finally(done);
    });

    it('posts an item to our channel', (done) => {
        let headers = {'Content-Type': 'text/plain'};
        let payload = moment().utc().toISOString();
        utils.httpPost(channelResource, headers, payload)
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                let itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
                postedItems.push(itemURL);
                console.log('itemURL:', itemURL);
            })
            .finally(done);
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
                    errors = []
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
            uris.forEach(uri => callbackItems.push(uri));
        }, done);
    });

    it('updates the webhook\'s callbackURL', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {
            channelUrl: channelResource,
            callbackUrl: callbackServerURL
        };
        utils.httpPut(webhookResource, headers, payload)
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(200);
                console.log('callbackURL:', fromObjectPath(['body', 'callbackUrl'], response));
            })
            .finally(done);
    });

    it('posts an item to our channel', (done) => {
        const headers = {'Content-Type': 'text/plain'};
        const payload = moment().utc().toISOString();
        utils.httpPost(channelResource, headers, payload)
            .then(response => {
                expect(getProp('statusCode', response)).toEqual(201);
                const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
                postedItems.push(itemURL);
                console.log('itemURL:', itemURL);
            })
            .finally(done);
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
