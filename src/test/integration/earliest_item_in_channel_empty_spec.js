require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function (channelResponse) {
    var earliestLink = channelResponse['_links']['earliest']['href'];
    frisby.create(testName + ':Test earliest item is 404 when channel is empty')
        .get(earliestLink)
        .expectStatus(404)
        .toss();
});

