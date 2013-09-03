require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = "latest_item_in_channel_is_404_when_channel_empty_spec";
utils.configureFrisby();

utils.runInTestChannel(channelName, function (channelResponse) {
    var latestLink = channelResponse['_links']['latest']['href'];
    frisby.create(testName + ':Test latest item is 404 when channel is empty')
        .get(latestLink)
        .expectStatus(404)
        .toss();
});

