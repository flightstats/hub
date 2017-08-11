require('../integration_config');

const channelName = utils.randomChannelName();
const channelURL = `${hubDomain}/channel/${channelName}`;

const webhookName = utils.randomChannelName();
const webhookURL = `${hubDomain}/webhook/${webhookName}`;

const contentTypeJSON = {'Content-Type': 'application/json'};
const contentTypePlain = {'Content-Type': 'text/plain'};

let callbackServer;
let callbackPort = utils.getPort();
let callbackURL = `${callbackDomain}:${callbackPort}`;
let callbackMessages = [];
let postedItems = [];

var webhookConfig = {
    callbackUrl: callbackURL,
    channelUrl: channelURL,
    parallel: 1,
    batch: 'SECOND',
    heartbeat: true
};


// this function will be available in the utils lib soon
const expectNoErrors = (error) => {
    expect(error).toBeNull();
};

describe(__filename, () => {

    it('creates a channel', (done) => {
        var body = {'name': channelName};
        utils.httpPost(channelUrl, contentTypeJSON, body)
            .then(response => {
                expect(response.statusCode).toEqual(201);
            })
            .catch(expectNoErrors)
            .fin(done);
    });

    it('runs a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackPort, (message) => {
            callbackMessages.push(message);
        }, done);
    });

    it('creates a webhook', (done) => {
        utils.httpPut(webhookURL, contentTypeJSON, webhookConfig)
            .then(response => {
                expect(response.statusCode).toEqual(201);
            })
            .catch(expectNoErrors)
            .fin(done);
    });

    let firstItemURL;
    it('inserts the first item', (done) => {
        utils.httpPost(channelURL, contentTypePlain, Date.now())
            .then(response => {
                expect(response.statusCode).toEqual(201);
                firstItemURL = respond.body._links.self.href;
                postedItems.push(firstItemURL);
            })
            .catch(expectNoErrors)
            .fin(done);
    });

    let secondItemURL;
    it('inserts the second item', (done) => {
        utils.httpPost(channelURL, contentTypePlain, Date.now())
            .then(response => {
                expect(response.statusCode).toEqual(201);
                secondItemURL = respond.body._links.self.href;
                postedItems.push(secondItemURL);
            })
            .catch(expectNoErrors)
            .fin(done);
    });

    it('waits for data', (done) => {
        utils.waitForData(callbackMessages, postedItems, done);
    });
    
    it('verifies we got the correct items', () => {
        expect(callbackMessages).toEqual(postedItems);
    });

    it('moves the cursor backward', (done) => {
        const backwardConfig = {
            'lastCompleted': firstItemURL
        };
        utils.httpPut(webhookURL, contentTypeJSON, backwardConfig)
            .then(response => {
                expect(response.statusCode).toEqual(201);
            })
            .catch(expectNoErrors)
            .fin(done);
    });

    it('waits for data', (done) => {
        const expectedItems = [firstItemURL, secondItemURL, secondItemURL];
        utils.waitForData(callbackMessages, expectedItems, done);
    });

    it('verifies we got the correct items', () => {
        expect(callbackMessages.length).toEqual(3);
        expect(callbackMessages[0]).toEqual(firstItemURL);
        expect(callbackMessages[1]).toEqual(secondItemURL);
        expect(callbackMessages[2]).toEqual(secondItemURL);
    });

    // TODO: test moving the cursor forward

    it('closes the callback server', (done) => {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

});
