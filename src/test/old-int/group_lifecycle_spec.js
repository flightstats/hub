require('./../integration/integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = callbackPort + 2;
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
    utils.createChannel(channelName);

    utils.putGroup(groupName, groupConfig);

    it('runs callback server', function () {
        var items = [];

        utils.startServer(port, function (string) {
            items.push(string);
        });

        runs(function () {
            for (var i = 0; i < 4; i++) {
                utils.postItem(channelResource);
            }

        });

        waitsFor(function () {
            return items.length == 4;
        }, 12000);

        utils.closeServer(function () {
            for (var i = 0; i < items.length; i++) {
                var parse = JSON.parse(items[i]);
                expect(parse.uris[0]).toBe(channelResource + '/100' + i);
                expect(parse.name).toBe(groupName);
            }
        });

    });

});

