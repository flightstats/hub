require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE'
};

var groupConfig2 = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'MINUTE',
    parallelCalls: 1
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    utils.getGroup(groupName, groupConfig2);

    utils.putGroup(groupName, groupConfig2, 200);

    utils.deleteGroup(groupName);

    utils.getGroup(groupName, groupConfig2, 404);
});

