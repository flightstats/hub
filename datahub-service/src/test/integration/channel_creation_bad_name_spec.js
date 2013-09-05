require('./integration_config.js');

var jsonBody = JSON.stringify({ "name": ''});

utils.configureFrisby();

frisby.create('Test create channel with empty name')
	.post(channelUrl, null, { body: jsonBody})
	.addHeader("Content-Type", "application/json")
	.expectStatus(400)
	.toss();



