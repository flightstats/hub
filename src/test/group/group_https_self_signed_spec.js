require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = 'https://' + ipAddress + ':' + port + '/';
var groupConfig = {
    callbackUrl : callbackUrl,
    channelUrl : channelResource
};

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a group on that channel
 * 3 - start a server at the https endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 */
describe(testName, function () {
    utils.createChannel(channelName);

    utils.putGroup(groupName, groupConfig);

    var callbackItems = [];
    var postedItems = [];
    var server;

    it('runs callback server', function (done) {
        server = utils.startHttpsServer(port, function (string) {
            callbackItems.push(string);
        }, done);

    });

    it('posts items', function () {
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
        }, 12000);

        function postedItem(value, post) {
            postedItems.push(value.body._links.self.href);
            if (post) {
                return utils.postItemQ(channelResource);
            }
        }
    });

    it('closes server and verifies items', function () {
        server.close();
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        for (var i = 0; i < callbackItems.length; i++) {
            var parse = JSON.parse(callbackItems[i]);
            expect(parse.uris[0]).toBe(postedItems[i]);
            expect(parse.name).toBe(groupName);
        }

    })

});

