require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function (channelResponse) {
    var latestLink = channelResponse['_links']['latest']['href'];
    frisby.create(testName + ':Test latest item is 404 when channel is empty')
        .get(latestLink)
        .expectStatus(404)
        .toss();
});

