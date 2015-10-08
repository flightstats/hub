require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false
};

var groupConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: false,
    batch: 'SINGLE',
    parallelCalls: 1
};

var groupConfig3 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    heartbeat: true,
    batch: 'SINGLE',
    parallelCalls: 1
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    utils.getGroup(groupName, groupConfig2);

    utils.putGroup(groupName, groupConfig3, 200);

    utils.getGroup(groupName, groupConfig3);

    utils.deleteGroup(groupName);

    utils.getGroup(groupName, groupConfig3, 404);
});

