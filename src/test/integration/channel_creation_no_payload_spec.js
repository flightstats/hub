require('./integration_config.js');

var testName = __filename;
utils.configureFrisby();

frisby.create(testName + ':Test create channel with empty name')
	.post(channelUrl, null, { body: '' })
	.addHeader("Content-Type", "application/json")
	.expectStatus(400)
	.toss();



