require('./integration_config.js');

utils.configureFrisby();

frisby.create('Test create channel with empty name')
	.post(channelUrl, null, { body: '' })
	.addHeader("Content-Type", "application/json")
	.expectStatus(400)
	.toss();



