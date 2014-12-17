require('./integration_config.js');

var channelName = utils.randomChannelName();
var jsonBody = JSON.stringify({ "name": channelName, "ttlMillis": null});
var expectedBody = {
    name: channelName,
    ttlDays : 2,
    "tags": [ "bar", "foo", "tagz"]
};
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

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
                    "ttlDays" : 2,
                    peakRequestRateSeconds: 5,
                    contentSizeKB: 20,
                    "tags": ["foo", "bar", "tagz"]
                };
                frisby.create(testName + ': Update channel ttlMillis')
                    .patch(channelResource, updateBody, {json:true})
                    .expectStatus(200)
                    .expectHeader('content-type', 'application/json')
                    .expectJSON(expectedBody)
                    .toss();

                frisby.create(testName + ': Update channel ttlMillis')
                    .get(channelResource)
                    .expectStatus(200)
                    .expectHeader('content-type', 'application/json')
                    .expectJSON(expectedBody)
                    .toss()

            })
            .toss()
    })
    .toss();



