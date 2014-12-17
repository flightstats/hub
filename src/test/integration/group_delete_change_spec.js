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

    var callbackItemsA = [];
    var callbackItemsB = [];
    var postedItemsA = [];
    var postedItemsB = [];
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
            callbackItemsA.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsA.push(value.body._links.self.href);
            });

        waitsFor(function () {
            return callbackItemsA.length == 1;
        }, 11999);

    });

    //todo - gfm - 12/17/14 - Build #76 (Dec 17, 2014 12:24:34 PM)
    //todo - gfm - 12/17/14 - Build #77 (Dec 17, 2014 12:31:07 PM) - test_0_959520191885531
    utils.deleteGroup(groupName);

    utils.addItem(channelResource);

    utils.putGroup(groupName, groupConfigB);

    it('runs callback server', function () {
        utils.startServer(portB, function (string) {
            callbackItemsB.push(string);
        });

        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItemsB.push(value.body._links.self.href);
            });

        waitsFor(function () {
            return callbackItemsB.length == 1;
            //todo - gfm - 12/17/14 - Build #76 (Dec 17, 2014 12:24:34 PM)
        }, 11998);

    });

    utils.closeServer(function () {
        expect(callbackItemsA.length).toBe(1);
        expect(callbackItemsB.length).toBe(1);
        expect(JSON.parse(callbackItemsA[0]).uris[0]).toBe(postedItemsA[0]);
        expect(JSON.parse(callbackItemsB[0]).uris[0]).toBe(postedItemsB[0]);
    });
});

