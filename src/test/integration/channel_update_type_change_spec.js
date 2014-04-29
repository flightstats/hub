require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName });
var channelResource = channelUrl + "/" + channelName;
var testName = 'channel_update_type_change_spec';

utils.configureFrisby();

frisby.create(testName + ': Making sure channel resource does not yet exist.')
    .get(channelResource)
    .expectStatus(404)
    .after(function () {
        frisby.create(testName + ': Test create channel')
            .post(channelUrl, null, { body: jsonBody})
            .addHeader("Content-Type", "application/json")
            .expectStatus(201)
            .afterJSON(function (result) {
                var updateBody = { type: "TimeSeries" };
                frisby.create(testName + ': fail to update channel type')
                    .patch(channelResource, updateBody, {json:true})
                    .expectStatus(200)
                    .expectJSON({ name: channelName, type: "Sequence"})
                    .toss()
            })
            .toss()
    })
    .toss();



