require('./integration_config.js');

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
 * 2 - create group on that channel at endpointA
 * 3 - start a server at endpointA
 * 4 - post item into the channel
 * 5 - delete the group
 * 6 - create the group with the same name and a different endpoint
 * 7 - start a server at endpointB
 * 8 - post item - should see item on endpointB
 */

describe(testName, function () {

    var portA = callbackPort + 4;
    var portB = callbackPort + 5;

    var itemsA = [];
    var itemsB = [];
    var groupConfigA = {
        callbackUrl : callbackDomain + ':' + portA + '/',
        channelUrl : channelResource
    };
    var groupConfigB = {
        callbackUrl : callbackDomain + ':' + portB + '/',
        channelUrl : channelResource
    };

    utils.createChannel(channelName);

    utils.putGroup(groupName, groupConfigA);

    it('runs callback server', function () {
        utils.startServer(portA, function (string) {
            itemsA.push(string);
        });

        runs(function () {
            utils.postItem(channelResource);
        });

        waitsFor(function () {
            return itemsA.length == 1;
        }, 12000);

    });

    utils.deleteGroup(groupName);

    utils.addItem(channelResource);

    utils.putGroup(groupName, groupConfigB);

    it('runs callback server', function () {
        utils.startServer(portB, function (string) {
            itemsB.push(string);
        });

        runs(function () {
            utils.postItem(channelResource);
        });

        waitsFor(function () {
            return itemsB.length == 1;
        }, 12000);

    });

    it('waits for item group ' + groupName + ' channel ' + channelName, function () {
        runs(function () {
            utils.postItem(channelResource);
        });

        waitsFor(function () {
            return itemsB.length == 1;
        }, 5000);

    });

    utils.closeServer(function () {
        expect(JSON.parse(itemsA[0]).uris[0]).toBe(channelResource + '/1000');
        expect(JSON.parse(itemsB[0]).uris[0]).toBe(channelResource + '/1002');
        expect(itemsA.length).toBe(1);
        expect(itemsB.length).toBe(1);
    });
});

