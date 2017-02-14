require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';

/**
 *
 * This should:
 *
 *  1 - create a channel
 *  2 - create a webhook with a callback timeout of 10 seconds
 *  3 - start a server at the endpoint
 *  4 - post items into the channel
 *  5 - verify the callback times out after 10 seconds
 *  6 - change the webhook callback timeout to 20 seconds
 *  7 - post items into the channel
 *  8 - verify that callback times out after 20 seconds
 *
 *  TODO ...
 *
 *  9 - change the webhook callback timeout to 0 seconds
 *  10 - verify the change failed (HTTP 400)
 *  11 - change the webhook callback timeout to 2000 seconds
 *  12 - verify the change failed (HTTP 400)
 */

describe(testName, function () {

    utils.createChannel(channelName);

    utils.putWebhook(webhookName, {
        callbackUrl: callbackUrl,
        channelUrl: channelResource,
        callbackTimeoutSeconds: 10
    }, 201, testName);

    it('verifies the callback times out after 10 seconds', function () {
        var serverTimeout = 15000; // 15s
        var serverTimedOut = false;
        var closed = false;

        utils.startCustomServer(port, function (request) {

            request.on('close', function () {
                closed = true;
            });

            request.socket.setTimeout(serverTimeout);
            request.socket.on('timeout', function () {
                serverTimedOut = true;
            });
        });

        utils.postItemQ(channelResource);

        waitsFor(function () {
            return closed;
        }, 'webhook callback didn\'t timeout as expected', serverTimeout);

        runs(function () {
            expect(closed).toBe(true);
            expect(serverTimedOut).toBe(false);
        });

        utils.closeServer();
    });

    utils.putWebhook(webhookName, {
        callbackUrl: callbackUrl,
        channelUrl: channelResource,
        callbackTimeoutSeconds: 20
    }, 201, testName);

    it('verifies the callback times out after 20 seconds', function () {
        var serverTimeout = 25000; // 25s
        var serverTimedOut = false;
        var closed = false;

        var server = utils.startCustomServer(port, function (request) {

            request.on('close', function () {
                closed = true;
            });

            request.socket.setTimeout(serverTimeout);
            request.socket.on('timeout', function () {
                serverTimedOut = true;
            });
        });

        utils.postItemQ(channelResource);

        waitsFor(function () {
            return closed;
        }, 'webhook callback didn\'t timeout as expected', serverTimeout);

        runs(function () {
            expect(closed).toBe(true);
            expect(serverTimedOut).toBe(false);
        });

        utils.closeServer();
    });

});
