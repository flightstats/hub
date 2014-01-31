require('./integration_config.js');

var jsonBody = JSON.stringify({ "name": "not valid!"});
var testName = "channel_creation_invalid_name_spec";

utils.configureFrisby();

frisby.create(testName + ': Test create channel with invalid name')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();



