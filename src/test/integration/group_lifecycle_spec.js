require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var groupConfig = {
    callbackUrl : callbackUrl,
    channelUrl : channelResource
};

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a group on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 */
describe(testName, function () {
    var callbackItems = [];
    var postedItems = [];

    utils.createChannel(channelName, false, testName);

    utils.putGroup(groupName, groupConfig, 201, testName);

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
                expect(parse.name).toBe(groupName);
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
        var groupResource = groupUrl + "/" + groupName;
        request.get({url : groupResource,
                headers : {"Content-Type" : "application/json"} },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(parse._links.self.href).toBe(groupResource);
                if (typeof groupConfig !== "undefined") {
                    expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                    expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                    expect(parse.transactional).toBe(groupConfig.transactional);
                    expect(parse.name).toBe(groupName);
                    expect(parse.lastCompletedCallback).toBe(postedItems[3]);
                }
                done();
        });

    })

});

