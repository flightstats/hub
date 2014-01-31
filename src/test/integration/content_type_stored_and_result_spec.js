require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = "content_type_stored_and_result_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    frisby.create(testName + ': Checking that the content-type is returned.')
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Type", "application/fractals")
        .expectStatus(201)
        .expectHeader('content-type', 'application/json')
        .expectJSON('_links', {
            channel: {
                href: thisChannelResource
            }
        })
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            frisby.create(testName + ': Fetching data and checking content-type header.')
                .get(valueUrl)
                .expectStatus(200)
                .expectHeader('content-type', 'application/fractals')
                .expectBodyContains(messageText)
                .toss();
        })
        .toss();
});

