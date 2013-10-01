require('./integration_config.js');

var jsonBody = JSON.stringify({ "name": ''});
var testName = 'channel_creation_bad_name_spec';

utils.configureFrisby();

frisby.create(testName + ': Test create channel with empty name')
	.post(channelUrl, null, { body: jsonBody})
	.addHeader("Content-Type", "application/json")
	.expectStatus(400)
	.toss();



