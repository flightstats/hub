require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere'
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig, 201, testName);

    var groupConfig2 = {
        callbackUrl : 'http://nothing/callback2',
        channelUrl : 'http://different/channel/notHere'
    };

    utils.putGroup(groupName, groupConfig2, 409, testName);

    utils.deleteGroup(groupName);

});

