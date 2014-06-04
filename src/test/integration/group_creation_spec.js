require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    transactional: false
};

var groupConfig2 = {
    callbackUrl : 'http://different/callback2',
    channelUrl: 'http://nothing/channel/notHere',
    transactional: true
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    utils.getGroup(groupName, groupConfig);

    utils.putGroup(groupName, groupConfig2, 200);

    utils.getGroup(groupName, groupConfig2);

    utils.deleteGroup(groupName);

});

