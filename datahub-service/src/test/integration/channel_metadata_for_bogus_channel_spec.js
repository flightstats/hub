require('./integration_config.js');

var channelName = "no_way_jose90928280xFF";
var thisChannelResource = channelUrl + "/" + channelName;
var testName = "channel_metadata_for_bogus_channel_spec";

utils.configureFrisby();

frisby.create(testName + ': Fetching channel metadata for nonexistent channel')
    .post(thisChannelResource)
    .expectStatus(404)
    .toss();
