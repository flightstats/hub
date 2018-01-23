require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';


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
    var postedItems = [];
    var firstItem;

    function addPostedItem(value) {
        postedItems.push(value.body._links.self.href);
        console.log('postedItems', postedItems);
    }

    it('posts initial items ' + channelResource, function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                firstItem = value.body._links.self.href;
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                done();
            });
    });

    it('creates a webhook', function (done) {
        var url = utils.getWebhookUrl() + '/' + webhookName;
        var headers = {'Content-Type': 'application/json'};
        var body = {
            callbackUrl: callbackUrl,
            channelUrl: channelResource,
            startItem: firstItem
        };

        utils.httpPut(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toBe(201);
                expect(response.headers.location).toBe(url);
                expect(response.body.callbackUrl).toBe(callbackUrl);
                expect(response.body.channelUrl).toBe(channelResource);
                expect(response.body.name).toBe(webhookName);
            })
            .finally(done);
    });

    var callbackServer;
    var callbackItems = [];

    it('starts a callback server', function (done) {
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            callbackItems.push(string);
        }, done);
    });
    
    it('inserts items', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('closes the first callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });
    
    it('verifies we got what we expected through the callback', function () {
        expect(callbackItems.length).toBe(5);
        expect(postedItems.length).toBe(5);
        for (var i = 0; i < callbackItems.length; i++) {
            var parse = JSON.parse(callbackItems[i]);
            expect(parse.uris[0]).toBe(postedItems[i]);
            expect(parse.name).toBe(webhookName);
        }
    });

});
