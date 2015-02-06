require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = callbackPort + 3;
var callbackUrl = callbackDomain + ':' + port + '/';


/**
 * This should:
 *
 * 1 - create a channel
 * 2 - add items to the channel
 * 2 - create a group on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the item are returned within delta time, excluding items posted in 2.
 */
describe(testName, function () {
    utils.createChannel(channelName);

    var postedItems = [];
    var firstItem;

    function postedItem(value, post) {
        postedItems.push(value.body._links.self.href);
        console.log('postedItems', postedItems);
        if (post) {
            return utils.postItemQ(channelResource);
        }
    }

    it('posts initial items', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                firstItem = value.body._links.self.href;
                return utils.postItemQ(channelResource);
            }).then(function (value) {
                postedItem(value, false);
                done();
            });
    });

    it('creates group', function (done) {
        var groupConfig = {
            callbackUrl: callbackUrl,
            channelUrl: channelResource,
            startItem: firstItem
        };
        var groupResource = groupUrl + "/" + groupName;
        console.log('creating group', groupName, groupConfig);
        request.put({
                url: groupResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(groupConfig)
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                expect(response.headers.location).toBe(groupResource);
                var parse = JSON.parse(body);
                expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                expect(parse.name).toBe(groupName);
                done();
            });
    });


    it('runs callback server', function () {
        var callbackItems = [];

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
            return callbackItems.length == 5;
        }, 11997);

        utils.closeServer(function () {
            expect(callbackItems.length).toBe(5);
            expect(postedItems.length).toBe(5);
            for (var i = 0; i < callbackItems.length; i++) {
                var parse = JSON.parse(callbackItems[i]);
                expect(parse.uris[0]).toBe(postedItems[i]);
                expect(parse.name).toBe(groupName);
            }
        });

    });

});

