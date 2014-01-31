require('./integration_config.js');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = "content_type_mismatch_spec";

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
            frisby.create(testName + ': Fetching data and validating mismatched content-type vs accept header.')
	            .addHeader("Accept", "application/json")
                .get(valueUrl)
                .expectStatus(406)
                .toss();
        })
        .toss();
});

