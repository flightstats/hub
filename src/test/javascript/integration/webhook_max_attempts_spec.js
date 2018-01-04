require('../integration_config');

const moment = require('moment');

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
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackServerPort, (request, response) => {
            let json = JSON.parse(request);
            console.log('callback server received item:', json);
            json.uris.forEach(uri => callbackItems.push(uri));
            response.statusCode = 400;
        }, done);
    });

    it('creates a webhook', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {
            channelUrl: channelResource,
            callbackUrl: callbackServerURL,
            callbackTimeoutSeconds: 1
        };
        utils.httpPut(webhookResource, headers, payload)
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('verify default max attempts is 0', (done) => {
        utils.httpGet(webhookResource)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                console.log('maxAttempts:', response.body.maxAttempts);
                expect(response.body.maxAttempts).toEqual(0);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('updates the max attempts to 1', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {maxAttempts: 1};
        utils.httpPut(webhookResource, headers, payload)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                console.log('maxAttempts:', response.body.maxAttempts);
                expect(response.body.maxAttempts).toEqual(1);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('posts an item to our channel', (done) => {
        let headers = {'Content-Type': 'text/plain'};
        let payload = moment().utc().toISOString();
        utils.httpPost(channelResource, headers, payload)
            .then(response => {
                expect(response.statusCode).toEqual(201);
                let itemURL = response.body._links.self.href;
                postedItems.push(itemURL);
                console.log('itemURL:', itemURL);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('waits for the callback server to receive the data', (done) => {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('waits for the webhook to give up', (done) => {
        setTimeout(done, 5 * 1000);
    });

    it('verifies we received the item only once', () => {
        console.log('callbackItems:', callbackItems);
        expect(callbackItems.length).toEqual(1);
    });

    it('verifies the webhook gave up after 1 attempt', (done) => {
        utils.httpGet(webhookResource)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                console.log(response.body);
                expect(response.body.lastCompleted).toEqual(response.body.channelLatest);
                expect(response.body.inFlight.length).toEqual(0);
                expect(response.body.errors.length).toEqual(2);
                let contentKey = postedItems[0].replace(`${channelResource}/`, '');
                expect(response.body.errors[0]).toContain(contentKey);
                expect(response.body.errors[0]).toContain('400 Bad Request');
                expect(response.body.errors[1]).toContain(`${contentKey} has reached max attempts (1)`);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

});
