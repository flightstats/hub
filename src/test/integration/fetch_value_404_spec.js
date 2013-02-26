require('./integration_config.js');
var frisby = require('frisby');
var utils = require('./utils.js');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/foooo" + Math.random().toString();

utils.runInTestChannel(channelName, function () {
    frisby.create('Fetching a nonexistent value.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});