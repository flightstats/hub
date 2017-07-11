require('../integration/integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: false
};

var webhookConfigPaused = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: true
};
/**
 *
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 * 6 - pause the webhook
 * 7 - post items into the channel
 * 8 - verify that no records are returned within delta time
 * 9 - un-pause the webhook
 * 10 - verify that the records are returned within delta time
 */
describe(testName, function () {

    var callbackItems = [];
    var postedItems = [];

    function postedItem(value, post) {
        postedItems.push(value.body._links.self.href);
        console.log('value.body._links.self.href', value.body._links.self.href)
        if (post) {
            return utils.postItemQ(channelResource);
        }
    }

    utils.createChannel(channelName);

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    it('runs callback server and posts ' + webhookName, function () {
        utils.startServer(port, function (string) {
            console.log('called back', string);
            callbackItems.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
            postedItem(value, false);
        });

        waitsFor(function () {
            return callbackItems.length == 2;
        }, 9999);


    });

    utils.putWebhook(webhookName, webhookConfigPaused, 200, testName);

    it('posts items to paused ' + webhookName, function () {

        utils.postItemQ(channelResource)
            .then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
            postedItem(value, false);
        });

        utils.sleep(10 * 1000);

    });

    it('verfies number ' + webhookName, function () {
        expect(callbackItems.length).toBe(2);

    });

    utils.putWebhook(webhookName, webhookConfig, 200, testName);

    it('waits for items ' + webhookName, function () {

        waitsFor(function () {
            return callbackItems.length == 4;
        }, 9998);

        utils.closeServer(function () {
            expect(callbackItems.length).toBe(4);
            expect(postedItems.length).toBe(4);
            for (var i = 0; i < callbackItems.length; i++) {
                var parse = JSON.parse(callbackItems[i]);
                expect(parse.uris[0]).toBe(postedItems[i]);
                expect(parse.name).toBe(webhookName);
            }
        }, testName);


    });

});

