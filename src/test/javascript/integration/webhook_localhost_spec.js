require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

var MINUTE = 60 * 1000


/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel with a localhost endpointA
 */

describe(testName, function () {

    var portB = utils.getPort();

    var itemsB = [];
    var postedItem;
    var badConfig = {
        callbackUrl: 'http://localhost:8080/nothing',
        channelUrl: channelResource
    };
    var webhookConfigB = {
        callbackUrl: callbackDomain + ':' + portB + '/',
        channelUrl: channelResource
    };

    utils.createChannel(channelName, false, testName);

    var MINUTE = 60 * 1000;

    it("executes large item suport", function (done) {
        var webhookResource = utils.getWebhookUrl() + "/" + webhookName;
        request.put({
                url: webhookResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(badConfig)
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(400);
                done();
            });
    }, 5 * MINUTE);

});
