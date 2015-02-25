require('./integration_config.js');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();

frisby.create(testName + ':Test create channel with empty name')
    .put(channelResource, null)
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .toss();

