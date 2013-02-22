require('./integration_config.js');
var frisby = require('frisby');
var utils = require('./utils.js');

var channelName = utils.randomChannelName();

utils.runInTestChannel(channelName, function (channelResponse) {
    console.info(channelResponse);
    var latestLink = channelResponse['_links']['latest']['href'];
    frisby.create('Test latest item is 404 when channel is empty')
        .get(latestLink)
        .expectStatus(404)
        .toss();
});

