require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere'
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    utils.getGroup(groupName, groupConfig);

    utils.putGroup(groupName, groupConfig, 200);

    utils.deleteGroup(groupName);

    utils.getGroup(groupName, groupConfig, 404);
});

