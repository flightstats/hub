require('./integration_config.js');

utils.configureFrisby();

var jsonBody = JSON.stringify({});
var testName = "channel_creation_no_name_spec";

frisby.create(testName + ':Test create channel with no name')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();



