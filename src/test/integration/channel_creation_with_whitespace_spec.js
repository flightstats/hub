require('./integration_config.js');

utils.configureFrisby();
var channelName = utils.randomChannelName();
var testName = "channel_creation_with_whitespace_spec";
var jsonBody = JSON.stringify({ "name": "    " + channelName + "    "});

frisby.create(testName + ':Test create channel with whitespace')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .expectHeader('location', channelUrl + "/" + channelName)
    .toss();



