require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName});
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();

frisby.create(testName + ': Making sure channel resource does not yet exist.')
    .get(channelResource)
    .expectStatus(404)
    .after(function () {
        frisby.create(testName + ': Test basic channel creation')
            .post(channelUrl, null, { body: jsonBody})
            .addHeader("Content-Type", "application/json")
            .expectStatus(201)
	        .expectHeader('content-type', 'application/json')
	        .expectHeader('location', channelResource)
            .expectJSON({
                _links: {
                    self: {
                        href: channelResource
                    }
                },
                name: channelName,
                ttlDays: 120,
                description: ""
                //TODO: Date validation
            })
            .afterJSON(function (result) {
                frisby.create(testName + ': Making sure channel resource exists.')
                    .get(result['_links']['self']['href'])
                    .expectStatus(200)
                    .toss();
            })
            .toss();

    })
    .toss();



