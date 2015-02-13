require('./integration_config.js');

var request = require('request');
var http = require('http');
var groupName = utils.randomChannelName();
var testName = __filename;
var groupConfig = {
    callbackUrl : 'http://nothing/callback',
    channelUrl : 'http://nothing/channel/notHere',
    parallelCalls : 2

};

describe(testName, function () {

    utils.putGroup(groupName, groupConfig);

    utils.getGroup(groupName, groupConfig);

});

