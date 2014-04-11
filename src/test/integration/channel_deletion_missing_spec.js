require('./integration_config.js');

var channelResource = hubUrlBase + "/channel/nonExistent";
var testName = "channel_deletion_missing_spec";
utils.configureFrisby();

frisby.create(testName + ': missing channel deletion')
    .delete(channelResource)
    .addHeader("Content-Type", "application/json")
    .expectStatus(404)
    .toss();
