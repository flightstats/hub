require('../integration_config');
var moment = require('moment');

const channelName = utils.randomChannelName();
// const channelURL = `http://${hubDomain}/channel/${channelName}`;
const channelResource = channelUrl + "/" + channelName;

const webhookName = utils.randomChannelName();
const webhookURL = `http://${hubDomain}/webhook/${webhookName}`;

const contentTypeJSON = {'Content-Type': 'application/json'};
const contentTypePlain = {'Content-Type': 'text/plain'};

let callbackServer;
let callbackPort = utils.getPort();
let callbackURL = `${callbackDomain}:${callbackPort}`;
let callbackMessages = [];
let postedItems = [];

let maxCursor = null;
let minCursor = null;

var webhookConfig = {
    callbackUrl: callbackURL,
    channelUrl: channelResource,
    parallelCalls: 1,
    batch: 'SECOND',
    heartbeat: true
};


describe(__filename, () => {
    it('runs a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackPort, (message) => {
            callbackMessages.push(message);
        }, done);
    });

    it('creates a channel', (done) => {
        var body = {'name': channelName};
        utils.httpPost(channelUrl, contentTypeJSON, body)
            .then(response => {
                expect(response.statusCode).toEqual(201);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    utils.putWebhook(webhookName, webhookConfig, 201, webhookURL)

    let firstItemURL;
    it('inserts the first item', (done) => {
        utils.httpPost(channelResource, contentTypePlain, "a test " + Date.now())
            .then(response => {
                expect(response.statusCode).toEqual(201);
                firstItemURL = response.body._links.self.href;
                postedItems.push(firstItemURL);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });


    utils.itSleeps(5000);

    // Check the latest on the webhook
    it('sets maxItem', (done) => {
        utils.httpGet(webhookURL, contentTypeJSON, false)
            .then(response => {
                var json = response.body;
                maxCursor = json.lastCompleted;
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('moves the cursor backward', (done) => {
        var y = moment().subtract(1, 'day');
        var formatted = y.format("YYYY/MM/DD/HH/mm/ss")
        var url = channelResource + "/" + formatted;
        console.log("backward cursor ", url)
        var item = {'item': url};
        utils.httpPut(webhookURL + "/updateCursor", contentTypePlain, url)
            .then(response => {
                expect(response.statusCode).toBeLessThan(300);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    utils.itSleeps(5000);

    it('checks to see if latest is before "maxCursor"', (done) => {
        utils.httpGet(webhookURL, contentTypeJSON, false)
            .then(response => {
                var json = response.body;
                expect(json.lastCompleted).toBeLessThan(maxCursor);
                minCursor = json.lastCompleted;
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('moves the cursor forward', (done) => {
        var y = moment().subtract(1, 'hour');
        var formatted = y.format("YYYY/MM/DD/HH/mm/ss")
        var url = channelResource + "/" + formatted;
        console.log("forward cursor ", url)
        var item = {'item': url};
        utils.httpPut(webhookURL + "/updateCursor", contentTypePlain, url)
            .then(response => {
                expect(response.statusCode).toBeLessThan(300);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    utils.itSleeps(5000);

    it('checks to see if latest is after "minCursor"', (done) => {
        utils.httpGet(webhookURL, contentTypeJSON, false)
            .then(response => {
                var json = response.body;
                expect(json.lastCompleted).toBeGreaterThan(minCursor);
                minCursor = json.lastCompleted;
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });


    it('closes the callback server', (done) => {
        expect(callbackServer).toBeDefined();

        callbackServer.close(() => {
            console.log("closed server....");
            done()
        });
        setImmediate(function () {
            callbackServer.emit('close')
        });
    });

});
