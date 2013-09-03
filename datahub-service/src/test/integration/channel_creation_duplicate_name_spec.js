require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName});
var channelResource = channelUrl + "/" + channelName;

utils.configureFrisby();

frisby.create('Making sure channel resource does not yet exist.')
    .get(channelResource)
    .expectStatus(404)
	.after(function () {
		frisby.create('Test create channel')
			.post(channelUrl, null, { body: jsonBody})
			.addHeader("Content-Type", "application/json")
			.expectStatus(201)
	        .after(function () {
				frisby.create('Test create channel with same name')
					.post(channelUrl, null, { body: jsonBody})
					.addHeader("Content-Type", "application/json")
					.expectStatus(409)
					.toss()
			})
            .toss();
    })
    .toss();



