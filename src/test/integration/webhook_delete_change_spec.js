require('../integration/integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
;


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
        callbackUrl : callbackDomain + ':' + portA + '/',
        channelUrl : channelResource
    };
    var webhookConfigB = {
        callbackUrl : callbackDomain + ':' + portB + '/',
        channelUrl : channelResource
    };

    utils.createChannel(channelName, false, testName);

    utils.putWebhook(webhookName, webhookConfigA, 201, testName);

    it('starts the first callback server', function (done) {
        callbackServerA = utils.startHttpServer(portA, function (string) {
            callbackItemsA.push(string);
        }, done);
    });

    it('posts the first item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsA.push(value.body._links.self.href);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItemsA, postedItemsA, done);
    });

    utils.deleteWebhook(webhookName);

    utils.itSleeps(5000);

    utils.putWebhook(webhookName, webhookConfigB, 201, testName);

    it('starts the second callback server', function (done) {
        callbackServerB = utils.startHttpServer(portB, function (string) {
            callbackItemsB.push(string);
        }, done);
    });

    it('posts the second item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsB.push(value.body._links.self.href);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItemsB, postedItemsB, done);
    });

    it('verifies we got what we expected through the callback', function () {
        expect(callbackItemsA.length).toBe(1);
        expect(callbackItemsB.length).toBe(1);
        expect(JSON.parse(callbackItemsA[0]).uris[0]).toBe(postedItemsA[0]);
        expect(JSON.parse(callbackItemsB[0]).uris[0]).toBe(postedItemsB[0]);
    });

    it('closes the first callback server', function (done) {
        expect(callbackServerA).toBeDefined();
        utils.closeServer(callbackServerA, done);
    });

    it('closes the second callback server', function (done) {
        expect(callbackServerB).toBeDefined();
        utils.closeServer(callbackServerB, done);
    });

});

