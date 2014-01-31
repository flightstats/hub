require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": "    " + channelName + "    "});
var testName = "channel_creation_with_whitespace_spec";

utils.configureFrisby();

frisby.create(testName + ':Test create channel with whitespace')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .expectHeader('location', channelUrl + "/" + channelName)
    .toss();



