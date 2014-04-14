require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName, "description": Array(1026).join("a")});
var testName = "channel_creation_invalid_description_spec";

utils.configureFrisby();

frisby.create(testName + ': Test create channel with invalid description')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();

