require('../integration_config');
const { createChannel, getProp, fromObjectPath } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
let createdChannel = false;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create webhook on that channel at endpointA
 * 3 - start a server at endpointA
 * 4 - post item into the channel
 * 5 - delete the webhook
 * 6 - create the webhook with the same name and a different endpoint
 * 7 - start a server at endpointB
 * 8 - post item - should see item on endpointB
 */

describe(testName, function () {

    var callbackServerA;
    var callbackServerB;
    var portA = utils.getPort();
    var portB = utils.getPort();

    var callbackItemsA = [];
    var callbackItemsB = [];
    var postedItemsA = [];
    var postedItemsB = [];
    var webhookConfigA = {
        callbackUrl: callbackDomain + ':' + portA + '/',
        channelUrl: channelResource,
    };
    var webhookConfigB = {
        callbackUrl: callbackDomain + ':' + portB + '/',
        channelUrl: channelResource,
    };

    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    utils.putWebhook(webhookName, webhookConfigA, 201, testName);

    it('starts the first callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServerA = utils.startHttpServer(portA, function (string) {
            callbackItemsA.push(string);
        }, done);
    });

    it('posts the first item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsA.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItemsA, postedItemsA, done);
    });

    utils.deleteWebhook(webhookName);

    utils.itSleeps(5000);

    utils.putWebhook(webhookName, webhookConfigB, 201, testName);

    it('starts the second callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServerB = utils.startHttpServer(portB, function (string) {
            callbackItemsB.push(string);
        }, done);
    });

    it('posts the second item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsB.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItemsB, postedItemsB, done);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackItemsA.length).toBe(1);
        expect(callbackItemsB.length).toBe(1);
        let uriA;
        let uriB;
        try {
            const itemA = JSON.parse(callbackItemsA[0]);
            const itemB = JSON.parse(callbackItemsB[0]);
            const urisA = getProp('uris', itemA);
            const urisB = getProp('uris', itemB);
            uriA = urisA && urisA[0];
            uriB = urisB && urisB[0];
        } catch (ex) {
            expect(`failed to parse json, ${ex}`).toBeNull();
        }
        expect(uriA).toBe(postedItemsA[0]);
        expect(uriB).toBe(postedItemsB[0]);
    });

    it('closes the first callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServerA).toBeDefined();
        utils.closeServer(callbackServerA, done);
    });

    it('closes the second callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServerB).toBeDefined();
        utils.closeServer(callbackServerB, done);
    });

});
