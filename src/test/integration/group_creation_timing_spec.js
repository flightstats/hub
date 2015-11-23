require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/notHere',
    ttlMinutes: 2,
    maxWaitMinutes: 10,
    batch: 'SINGLE'

};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig, 201, testName);

    utils.getGroup(groupName, groupConfig);

});

