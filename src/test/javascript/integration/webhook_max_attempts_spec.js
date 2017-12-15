require('../integration_config');

const moment = require('moment');

const channelResource = `${channelUrl}/${utils.randomChannelName()}`;
const webhookResource = `${utils.getWebhookUrl()}/${utils.randomChannelName()}`;

/*
This test:
creates a channel
starts a webhook endpoint
the endpoint records incoming requests, and responds with a 400
create a webhook with maxAttempts = 0
change maxAttempts to 1
verify that maxAttempts == 1
post an item to the channel
verify that the item was sent
verify that webhook still has 'initial' as lastCompleted
verify that webhook has no inFlight
 */

describe(__filename, () => {

    let postedItem;

    it('creates a channel', (done) => {
        utils.httpPut(channelResource)
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('create a webhook', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {
            channelUrl: channelResource,
            callbackUrl: 'http://not.a.real.server/',
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
                postedItem = itemURL;
                console.log('itemURL:', itemURL);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('waits for the webhook to give up', (done) => {
        setTimeout(done, 15 * 1000);
    });

    it('verifies the webhook gave up after 1 attempt', (done) => {
        utils.httpGet(webhookResource)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                console.log(response.body);
                expect(response.body.lastCompleted).toEqual(response.body.channelLatest);
                expect(response.body.inFlight.length).toEqual(0);
                expect(response.body.errors.length).toEqual(2);
                let contentKey = postedItem.replace(`${channelResource}/`, '');
                expect(response.body.errors[0]).toContain(`${contentKey} java.net.UnknownHostException: not.a.real.server`);
                expect(response.body.errors[1]).toContain(`${contentKey} has reached max attempts (1)`);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
