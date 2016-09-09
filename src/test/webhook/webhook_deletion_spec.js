require('../integration/integration_config.js');

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
 * 4 - post item into the channel
 * 5 - delete the group
 * 6 - recreate the group
 * 7 - post item - should only see new item
 */
describe(testName, function () {
    var callbackItems = [];
    var postedItems = [];

    utils.createChannel(channelName, false, testName);

    utils.putGroup(groupName, groupConfig, 201, testName);

    it('waits', function (done) {
        setTimeout(function () {
            done();
        }, 500);
    });

    it('runs callback server', function () {
        utils.startServer(port, function (string) {
            callbackItems.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(value.body._links.self.href);
            });

        waitsFor(function () {
            return callbackItems.length == 1;
        }, 18333);

    });

    utils.deleteGroup(groupName);

    utils.addItem(channelResource);

    utils.putGroup(groupName, groupConfig, 201, testName);

    it('waits', function (done) {
        setTimeout(function () {
            done();
        }, 500);
    });

    it('waits for item group ' + groupName + ' channel ' + channelName, function () {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(value.body._links.self.href);
            });

        waitsFor(function () {
            return callbackItems.length == 2;
        }, 18444);

    });

    utils.closeServer(function () {
        expect(callbackItems.length).toBe(2);
        expect(JSON.parse(callbackItems[0]).uris[0]).toBe(postedItems[0]);
        expect(JSON.parse(callbackItems[1]).uris[0]).toBe(postedItems[1]);
    }, testName);
});

