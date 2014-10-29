require('./../integration/integration_config.js');

var channelName = utils.randomChannelName();
var testName = "latest_item_in_nonexistant_channel_spec";

utils.configureFrisby();

var latestLink = hubUrlBase + "/channel/" + channelName + "/latest";
frisby.create(testName + ":Test latest item is 404 when channel dosn't exist")
    .get(latestLink)
    .expectStatus(404)
    .toss();

