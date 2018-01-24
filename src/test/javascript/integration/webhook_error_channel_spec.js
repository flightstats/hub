require('../integration_config');

const moment = require('moment');

const dataChannelName = utils.randomChannelName();
const dataChannelURL = `${hubUrlBase}/channel/${dataChannelName}`;
const errorChannelName = utils.randomChannelName();
const errorChannelURL = `${hubUrlBase}/channel/${errorChannelName}`;
const callbackServerPort = utils.getPort();
const callbackServerURL = `${callbackDomain}:${callbackServerPort}`;
const webhookName = utils.randomChannelName();
const webhookURL = `${hubUrlBase}/webhook/${webhookName}`;

describe(__filename, () => {

    let callbackServer;
    let callbackItems = [];
    let postedItems = [];

    it('creates a data channel', (done) => {
        utils.httpPut(dataChannelURL)
            .then(response => expect(response.statusCode).toEqual(201))
            .finally(done);
    });

    it('creates an error channel', (done) => {
        utils.httpPut(errorChannelURL)
            .then(response => expect(response.statusCode).toEqual(201))
            .finally(done);
    });

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackServerPort, (request) => {
            let json = JSON.parse(request);
            console.log('incoming:', json);
            json.uris.forEach(uri => callbackItems.push(uri));
        }, done);
    });

    it('creates a webhook', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {
            channelUrl: dataChannelURL,
            callbackUrl: callbackServerURL,
            errorChannelUrl: errorChannelURL,
            callbackTimeoutSeconds: 10,
            maxAttempts: 1
        };
        utils.httpPut(webhookURL, headers, payload)
            .then(response => expect(response.statusCode).toEqual(201))
            .finally(done);
    });

    it('posts an item to the data channel', (done) => {
        let headers = {'Content-Type': 'text/plain'};
        let payload = moment.utc().toISOString();
        utils.httpPost(dataChannelURL, headers, payload)
            .then(response => {
                expect(response.statusCode).toEqual(201);
                let itemURL = response.body._links.self.href;
                postedItems.push(itemURL);
            })
            .finally(done);
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies no error posted to the error channel', (done) => {
        utils.httpGet(`${errorChannelURL}/latest`)
            .then(utils.followRedirectIfPresent)
            .then(response => expect(response.statusCode).toEqual(404))
            .finally(done);
    });

    it('kills the callback server', (done) => {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    let postedTime;

    it('posts an item to the data channel', (done) => {
        let headers = {'Content-Type': 'text/plain'};
        let payload = moment.utc().toISOString();
        utils.httpPost(dataChannelURL, headers, payload)
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
                postedTime = moment.utc();
                console.log('postedTime:', postedTime.toISOString());
            })
            .finally(done);
    });

    let giveUpTime;

    it('waits for the webhook to give up', (done) => {
        let timeoutMS = 10 * 1000;
        let clause = (response) => response.body.errors.filter(e => e.includes('max attempts reached')).length > 0;
        utils.httpGetUntil(webhookURL, clause, timeoutMS)
            .then(response => {
                giveUpTime = moment.utc();
                console.log('giveUpTime:', giveUpTime.toISOString());
            })
            .catch(error => done.fail(error))
            .finally(done);
    });

    it('verifies an error was posted to the error channel', (done) => {
        expect(postedTime).toBeDefined();
        expect(giveUpTime).toBeDefined();
        let timeoutMS = 10 * 1000;
        let clause = (response) => response.statusCode === 303;
        utils.httpGetUntil(`${errorChannelURL}/latest`, clause, timeoutMS)
            .then(utils.followRedirectIfPresent)
            .then(response => {
                console.log(response.body);
                expect(response.body.webhook).toEqual(webhookURL);
                expect(response.body.failedItem).toEqual(postedItems[1]);
                expect(response.body.numberOfAttempts).toEqual(1);
                expect(moment(response.body.lastAttemptTime)).toBeGreaterThan(postedTime);
                expect(moment(response.body.lastAttemptTime)).toBeLessThan(giveUpTime);
                expect(response.body.lastAttemptError).toEqual("max attempts reached (1)");
            })
            .finally(done);
    });

});
