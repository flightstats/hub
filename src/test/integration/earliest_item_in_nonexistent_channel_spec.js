require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = __filename;

utils.configureFrisby();

var earliestLink = hubUrlBase + "/channel/" + channelName + "/earliest";
frisby.create(testName + ":Test earliest item is 404 when channel dosn't exist")
    .get(earliestLink)
    .expectStatus(404)
    .toss();

