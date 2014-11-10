require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name" : channelName, "ttlMillis" : 0});
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

utils.configureFrisby();

frisby.create(testName + ':Test create channel with zero TTL Days')
    .post(channelUrl, null, { body : jsonBody })
    .addHeader("Content-Type", "application/json")
    .expectStatus(201)
    .toss();




