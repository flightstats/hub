require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere'
};

var groupConfig2 = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere',
    parallelCalls: 1,
    batch: 'SINGLE'
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig, 201, testName);

    utils.getGroup(groupName, groupConfig2);

    /*utils.putGroup(groupName, groupConfig2, 200, testName);

    utils.deleteGroup(groupName);

     utils.getGroup(groupName, groupConfig2, 404);*/
});

