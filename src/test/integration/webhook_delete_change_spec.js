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

    it('runs callback server', function () {
        utils.startServer(portA, function (string) {
            callbackItemsA.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsA.push(value.body._links.self.href);
            });

        waitsFor(function () {
            return callbackItemsA.length == 1;
        }, 11999);

    });

    utils.deleteWebhook(webhookName);

    utils.itSleeps(5000);

    utils.putWebhook(webhookName, webhookConfigB, 201, testName);

    it('runs callback server', function () {
        utils.startServer(portB, function (string) {
            callbackItemsB.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsB.push(value.body._links.self.href);
            });

        waitsFor(function () {
            return callbackItemsB.length == 1;
        }, 11998);

    });

    utils.closeServer(function () {
        expect(callbackItemsA.length).toBe(1);
        expect(callbackItemsB.length).toBe(1);
        expect(JSON.parse(callbackItemsA[0]).uris[0]).toBe(postedItemsA[0]);
        expect(JSON.parse(callbackItemsB[0]).uris[0]).toBe(postedItemsB[0]);
    }, testName);
});

