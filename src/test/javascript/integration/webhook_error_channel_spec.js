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
    let postedTime;
    let giveUpTime;

    it('creates a data channel', (done) => {
        utils.httpPut(dataChannelURL)
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('creates an error channel', (done) => {
        utils.httpPut(errorChannelURL)
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('creates a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackServerPort, function (string) {
            callbackItems.push(string);
        }, done);
    });

    it('creates a webhook', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let payload = {
            channelUrl: dataChannelURL,
            callbackUrl: callbackServerURL,
            errorChannelUrl: errorChannelURL,
            callbackTimeoutSeconds: 10,
            maxAttempts: 1 // todo: replace with the correct property name once #986 is complete
        };
        utils.httpPut(webhookURL, headers, payload)
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('posts an item to the data channel', (done) => {
        let headers = {'Content-Type': 'text/plain'};
        let payload = moment.utc().toISOString();
        utils.httpPost(dataChannelURL, headers, payload)
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies no error posted to the error channel', (done) => {
        utils.httpGet(`${errorChannelURL}/latest`)
            .then(response => expect(response.statusCode).toEqual(404))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('kills the callback server', (done) => {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    it('posts an item to the data channel', (done) => {
        let headers = {'Content-Type': 'text/plain'};
        let payload = moment.utc().toISOString();
        utils.httpPost(dataChannelURL, headers, payload)
            .then(response => {
                expect(response.statusCode).toEqual(201);
                postedItems.push(response.body._links.self.href);
                postedTime = moment.utc();
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('waits for the webhook to give up', (done) => {
        let tenSeconds = 10 * 1000;
        setTimeout(() => {
            giveUpTime = moment.utc();
            done();
        }, tenSeconds);
    });

    it('verifies an error was posted to the error channel', (done) => {
        expect(postedTime).toBeDefined();
        expect(giveUpTime).toBeDefined();
        utils.httpGet(`${errorChannelURL}/latest`)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                expect(response.body.itemURL).toEqual(postedItems[1]);
                expect(response.body.webhookURL).toEqual(webhookURL);
                expect(response.body.attempts).toEqual(1);
                expect(moment(response.body.lastAttemptTime)).toBeGreaterThan(postedTime);
                expect(moment(response.body.lastAttemptTime)).toBeLessThan(giveUpTime);
                // todo: figure out what error we'll get
                expect(response.body.lastAttemptError).toEqual('blah');
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
