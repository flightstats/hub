require('./integration_config.js');

var channelName = utils.randomChannelName();

utils.configureFrisby();

utils.runInTestChannel(channelName, function (channelResponse) {
    var latestLink = channelResponse['_links']['latest']['href'];
    frisby.create('Test latest item is 404 when channel is empty')
        .get(latestLink)
        .expectStatus(404)
        .toss();
});

