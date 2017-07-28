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
    utils.itSleeps(1000);
    utils.addItem(channelResource);
    utils.addItem(channelResource);

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    var callbackServer;
    var callbackItems = [];
    var postedItems = [];

    it('runs callback server', function (done) {
        callbackServer = utils.startHttpsServer(port, function (string) {
            callbackItems.push(string);
        }, done);

    });

    it('posts items', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(value.body._links.self.href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value.body._links.self.href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value.body._links.self.href);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value.body._links.self.href);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('closes the callback server', function (done) {
        utils.closeServer(callbackServer, done);
    });

    it('verifies we got what we expected through the callback', function () {
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        for (var i = 0; i < callbackItems.length; i++) {
            var parse = JSON.parse(callbackItems[i]);
            expect(parse.uris[0]).toBe(postedItems[i]);
            expect(parse.name).toBe(webhookName);
        }
    });

});

