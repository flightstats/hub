require('./integration_config.js');
var frisby = require('frisby');
var utils = require('utils');

var channelName = utils.randomChannelName();
var badValueUrl = channelUrl + "/" + channelName + "/foooo" + Math.random().toString();

utils.runInTestChannel(channelName, function () {
    console.info('Fetching a nonexistent value...');
    frisby.create('Fetching a nonexistent value.')
        .get(badValueUrl)
        .expectStatus(404)
        .toss();
});