require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName, "ttlMillis": 0});
var channelResource = channelUrl + "/" + channelName;
var testName = 'channel_creation_with_zero_ttl_spec';

utils.configureFrisby();

frisby.create(testName + ':Test create channel with zero TTL')
    .post(channelUrl, null, { body: jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();




