require('../integration_config');
const {
    getProp,
    fromObjectPath,
    hubClientGet,
    hubClientPost,
    hubClientPut,
} = require('../lib/helpers');
const moment = require('moment');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const webhookName = utils.randomChannelName();
const webhookURL = `http://${hubDomain}/webhook/${webhookName}`;

const contentTypeJSON = { 'Content-Type': 'application/json' };
const contentTypePlain = { 'Content-Type': 'text/plain' };

let callbackServer;
const callbackPort = utils.getPort();
const callbackMessages = [];
const postedItems = [];

let maxCursor = null;
let minCursor = null;
let firstItemURL = null;
const webhookConfig = {
    callbackUrl: `${callbackDomain}:${callbackPort}`,
    channelUrl: channelResource,
    parallelCalls: 1,
    batch: 'SECOND',
    heartbeat: true,
};

describe(__filename, () => {
    it('runs a callback server', (done) => {
        callbackServer = utils.startHttpServer(callbackPort, (message) => {
            callbackMessages.push(message);
        }, done);
    });

    it('creates a channel', async () => {
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, contentTypeJSON, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.putWebhook(webhookName, webhookConfig, 201, webhookURL);

    it('inserts the first item', async () => {
        const response = await hubClientPost(channelResource, contentTypePlain, "a test " + Date.now());
        expect(getProp('statusCode', response)).toEqual(201);
        firstItemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
        postedItems.push(firstItemURL);
    });

    utils.itSleeps(5000);

    // Check the latest on the webhook
    it('sets maxItem', async () => {
        const response = await hubClientGet(webhookURL, contentTypeJSON);
        maxCursor = fromObjectPath(['body', 'lastCompleted'], response);
    });

    it('moves the cursor backward', async () => {
        const y = moment().subtract(1, 'day');
        const formatted = y.format("YYYY/MM/DD/HH/mm/ss");
        const url = channelResource + "/" + formatted;
        console.log("backward cursor ", url);
        const response = await hubClientPut(webhookURL + "/updateCursor", contentTypePlain, url);
        expect(getProp('statusCode', response)).toBeLessThan(300);
    });

    utils.itSleeps(5000);

    it('checks to see if latest is before "maxCursor"', async () => {
        const response = await hubClientGet(webhookURL, contentTypeJSON, false);
        const lastCompleted = fromObjectPath(['body', 'lastCompleted'], response);
        expect(lastCompleted).toBeLessThan(maxCursor);
        minCursor = lastCompleted;
    });

    it('moves the cursor forward', async () => {
        const y = moment().subtract(1, 'hour');
        const formatted = y.format("YYYY/MM/DD/HH/mm/ss");
        const url = channelResource + "/" + formatted;
        console.log("forward cursor ", url);
        // const item = { 'item': url };
        const response = await hubClientPut(webhookURL + "/updateCursor", contentTypePlain, url);
        expect(getProp('statusCode', response)).toBeLessThan(300);
    });

    utils.itSleeps(5000);

    it('checks to see if latest is after "minCursor"', async () => {
        const response = await hubClientGet(webhookURL, contentTypeJSON, false);
        const lastCompleted = fromObjectPath(['body', 'lastCompleted'], response);
        expect(lastCompleted).toBeGreaterThan(minCursor);
        minCursor = lastCompleted;
    });

    it('closes the callback server', (done) => {
        expect(callbackServer).toBeDefined();

        callbackServer.close(() => {
            console.log("closed server....");
            done();
        });
        setImmediate(function () {
            callbackServer.emit('close');
        });
    });
});
