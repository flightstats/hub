require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName, "ttlMillis": null});
var channelResource = channelUrl + "/" + channelName;
var testName = 'channel_update_spec';

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
                var updateBody = {
                    "ttlMillis": 60000,
                    peakRequestRateSeconds: 5,
                    contentSizeKB: 20
                };
                frisby.create(testName + ': Update channel ttlMillis')
                    .patch(channelResource, updateBody, {json:true})
                    .expectStatus(200)
                    .expectHeader('content-type', 'application/json')
                    .expectJSON({
                        name: channelName,
                        type: "Sequence",
                        contentSizeKB: 20,
                        peakRequestRateSeconds: 5,
                        ttlMillis: 60000
                    })
                    .toss()
            })
            .toss()
    })
    .toss();



