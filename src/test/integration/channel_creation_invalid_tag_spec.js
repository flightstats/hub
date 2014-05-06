require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName, "tags": ["foo bar", "bar@home", "tagz"]});
var channelResource = channelUrl + "/" + channelName;
var testName = 'channel_creation_invalid_tag_spec';

utils.configureFrisby();


frisby.create(testName + 'Test create channel with invalid tag')
    .post(channelUrl, null, { body: jsonBody})
    .addHeader("Content-Type", "application/json")
    .expectStatus(400)
    .toss();

