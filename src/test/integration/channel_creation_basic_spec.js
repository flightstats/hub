require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName});
var channelResource = channelUrl + "/" + channelName;

utils.configureFrisby();

frisby.create('Making sure channel resource does not yet exist.')
    .get(channelResource)
    .expectStatus(404)
    .after(function () {
        frisby.create('Test basic channel creation')
            .post(channelUrl, null, { body: jsonBody})
            .addHeader("Content-Type", "application/json")
            .expectStatus(200)
            .expectHeader('content-type', 'application/json')
            .expectJSON({
                _links: {
                    self: {
                        href: channelResource
                    }
                },
                name: channelName
                //TODO: Date validation
            })
            .afterJSON(function (result) {
                frisby.create('Making sure channel resource exists.')
                    .get(result['_links']['self']['href'])
                    .expectStatus(200)
                    .toss();
            })
            .toss();

    })
    .toss();



