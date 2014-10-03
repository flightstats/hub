require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = callbackPort + 1;
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
 * 4 - post item into the channel
 * 5 - delete the group
 * 6 - recreate the group
 * 7 - post item - should only see new item
 */
describe(testName, function () {
    var items = [];

    utils.createChannel(channelName);

    utils.putGroup(groupName, groupConfig);

    it('runs callback server', function () {
        utils.startServer(port, function (string) {
            items.push(string);
        });

        runs(function () {
            utils.postItem(channelResource);
        });

        waitsFor(function () {
            return items.length == 1;
        }, 12000);

    });

    utils.deleteGroup(groupName);

    utils.addItem(channelResource);

    utils.putGroup(groupName, groupConfig);

    it('waits for item group ' + groupName + ' channel ' + channelName, function () {
        runs(function () {
            utils.postItem(channelResource);
        });

        waitsFor(function () {
            return items.length == 2;
        }, 5000);

    });

    utils.closeServer(function () {
        expect(JSON.parse(items[0]).uris[0]).toBe(channelResource + '/1000');
        expect(JSON.parse(items[1]).uris[0]).toBe(channelResource + '/1002');
        expect(items.length).toBe(2);
    });
});

