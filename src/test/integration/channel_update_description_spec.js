require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName, "description": "starting"});
var channelResource = channelUrl + "/" + channelName;
var testName = 'channel_update_ttl_null_spec';
utils.configureFrisby();

frisby.create(testName + ': Making sure channel resource does not yet exist.')
    .get(channelResource)
    .expectStatus(404)
    .after(function () {
        frisby.create(testName + ': Test create channel')
            .post(channelUrl, null, { body: jsonBody})
            .addHeader("Content-Type", "application/json")
            .expectStatus(201)
            .expectJSON({"description": "starting"})
            .afterJSON(function (result) {
                var updateBody = {"description": "next description"};
                frisby.create(testName + ': Update channel description')
                    .patch(channelResource, updateBody, {json:true})
                    .expectStatus(200)
                    .expectHeader('content-type', 'application/json')
                    .expectJSON({"name": channelName})
                    .expectJSON({"description": "next description"})
                    .toss()
            })
            .toss()
    })
    .toss();

