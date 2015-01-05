require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = __filename;

utils.configureFrisby();

var latestLink = hubUrlBase + "/channel/" + channelName + "/latest";
frisby.create(testName + ":Test latest item is 404 when channel dosn't exist")
    .get(latestLink)
    .expectStatus(404)
    .toss();

