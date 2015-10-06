require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    batch: 'SINGLE'
};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    var groupConfig2 = {
        callbackUrl: 'http://nothing/callback',
        channelUrl: 'http://nothing/channel/notHere',
        batch: 'MINUTE'
    };

    utils.putGroup(groupName, groupConfig2, 200);

    utils.deleteGroup(groupName);

});

