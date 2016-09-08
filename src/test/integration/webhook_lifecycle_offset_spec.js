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
 * 2 - add items to the channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the item are returned within delta time, excluding items posted in 2.
 */
describe(testName, function () {
    utils.createChannel(channelName, false, testName);
    utils.timeout(1000);
    utils.addItem(channelResource);
    utils.addItem(channelResource);

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    it('runs callback server', function () {
        var callbackItems = [];
        var postedItems = [];

        utils.startServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
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
        }, 11997);

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

});

