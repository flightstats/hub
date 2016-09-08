require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var webhookConfig = {
    callbackUrl : callbackUrl,
    channelUrl : channelResource
};

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 */
describe(testName, function () {
    var callbackItems = [];
    var postedItems = [];

    utils.createChannel(channelName, false, testName);

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    it('runs callback server', function () {
        utils.startServer(port, function (string) {
            callbackItems.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                postedItem(value, false);
            });

        waitsFor(function () {
            return callbackItems.length == 4;
        }, 9999);

        utils.closeServer(function () {
            expect(callbackItems.length).toBe(4);
            expect(postedItems.length).toBe(4);
            for (var i = 0; i < callbackItems.length; i++) {
                var parse = JSON.parse(callbackItems[i]);
                expect(parse.uris[0]).toBe(postedItems[i]);
                expect(parse.name).toBe(webhookName);
            }
        }, testName);

        function postedItem(value, post) {
            postedItems.push(value.body._links.self.href);
            if (post) {
                return utils.postItemQ(channelResource);
            }
        }

    });

    it('verifies lastCompleted', function (done) {
        var webhookResource = utils.getWebhookUrl() + "/" + webhookName;
        request.get({
                url: webhookResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(parse._links.self.href).toBe(webhookResource);
                if (typeof webhookConfig !== "undefined") {
                    expect(parse.callbackUrl).toBe(webhookConfig.callbackUrl);
                    expect(parse.channelUrl).toBe(webhookConfig.channelUrl);
                    expect(parse.transactional).toBe(webhookConfig.transactional);
                    expect(parse.name).toBe(webhookName);
                    expect(parse.lastCompletedCallback).toBe(postedItems[3]);
                }
                done();
        });

    })

});

