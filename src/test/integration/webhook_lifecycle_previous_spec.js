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
 * This should:
 *
 * 1 - create a channel
 * 2 - add items to the channel
 * 2 - create a webhook on that channel with startItem=previous
 * 3 - start a server at the endpoint
 * 4 - post item into the channel
 * 5 - verify that the item are returned within delta time, incuding the second item posted in 2.
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
    
    utils.itSleeps(6000);

    it('creates webhook ' + webhookName, function (done) {
        var webhookConfig = {
            callbackUrl: callbackUrl,
            channelUrl: channelResource,
            startItem: 'previous'
        };
        var webhookResource = utils.getWebhookUrl() + "/" + webhookName;
        console.log('creating webhook', webhookName, webhookConfig);
        request.put({
                url: webhookResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(webhookConfig)
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                expect(response.headers.location).toBe(webhookResource);
                var parse = utils.parseJson(response, testName);
                expect(parse.callbackUrl).toBe(webhookConfig.callbackUrl);
                expect(parse.channelUrl).toBe(webhookConfig.channelUrl);
                expect(parse.name).toBe(webhookName);
                done();
            });
    });


    it('runs callback server webhook:' + webhookName + ' channel:' + channelName, function () {
        var callbackItems = [];

        utils.startServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            callbackItems.push(string);
        });

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
            });

        waitsFor(function () {
            return callbackItems.length == 5;
        }, 11997);

        utils.closeServer(function () {
            expect(callbackItems.length).toBe(5);
            expect(postedItems.length).toBe(5);
            for (var i = 0; i < callbackItems.length; i++) {
                var parse = JSON.parse(callbackItems[i]);
                expect(parse.uris[0]).toBe(postedItems[i]);
                expect(parse.name).toBe(webhookName);
            }
        }, testName);

    });

});

