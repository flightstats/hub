require('./integration_config.js');

var channelName = '123_you_aint_gunna_find_me';
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "Any old value!";
var testName = "insert_to_nonexistent_channel_spec";

utils.configureFrisby();

frisby.create(testName + ': Inserting a value into a bogus channel.')
    .post(thisChannelResource, null, { body: messageText})
    .addHeader("Content-Type", "text/plain")
    .expectStatus(404)
    .toss();
