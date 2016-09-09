require('../integration/integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
;


/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a group on that channel with a non-existent endpointA
 * 3 - post item into the channel
 * 4 - change the group with the same name and a new endpointB
 * 5 - start a server at the endpointB
 * 6 - post item - should see items at endPointB
 */

describe(testName, function () {

    var portB = utils.getPort();

    var itemsB = [];
    var postedItem;
    var badConfig = {
        callbackUrl: 'http://localhost:8080/nothing',
        channelUrl: channelResource
    };
    var groupConfigB = {
        callbackUrl: callbackDomain + ':' + portB + '/',
        channelUrl: channelResource
    };

    utils.createChannel(channelName, false, testName);

    utils.putGroup(groupName, badConfig, 201, testName);

    utils.addItem(channelResource);

    utils.putGroup(groupName, groupConfigB, 200, testName);

    it('runs callback server: channel:' + channelName + ' group:' + groupName, function () {
        utils.startServer(portB, function (string) {
            console.log('called group ' + groupName + ' ' + string);
            itemsB.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItem = value.body._links.self.href;
            });

        waitsFor(function () {
            return itemsB.length == 2;
        }, 20002);

    });

    utils.closeServer(function () {
        expect(itemsB.length).toBe(2);
        expect(JSON.parse(itemsB[1]).uris[0]).toBe(postedItem);
    }, testName);
});

