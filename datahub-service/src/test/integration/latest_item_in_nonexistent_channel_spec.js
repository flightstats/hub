require('./integration_config.js');

var channelName = utils.randomChannelName();
var testName = "latest_item_in_nonexistant_channel_spec";

utils.configureFrisby();

var latestLink = dataHubUrlBase + "/channel/"+channelName+"/latest";
console.log(latestLink);
frisby.create(testName + ":Test latest item is 404 when channel dosn't exist")
    .get(latestLink)
    .expectStatus(404)
    .toss();

