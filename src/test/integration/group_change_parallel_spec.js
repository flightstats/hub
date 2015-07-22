require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfigA = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 1

};

var groupConfigB = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    parallelCalls: 2

};

describe(testName, function () {

    utils.putGroup(groupName, groupConfigA);

    utils.getGroup(groupName, groupConfigA);

    utils.putGroup(groupName, groupConfigB, 200);

    utils.getGroup(groupName, groupConfigB);

});

