require('./integration_config.js');
var request = require('request');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "Testing that the Content-Encoding header is returned";

var testName = "content_encoding_header_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    // Note: We have to use request directly here, because Frisby insists on having a content-type specified.
    frisby.create(testName + ": Testing the content-encoding header")
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Encoding", "gzip")
        .expectStatus(201)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            console.info("yeah " + valueUrl);
            frisby.create("Fetching to confirm header")
                .get(valueUrl)
                .expectHeader('content-encoding', 'gzip')
                .toss()
        })
        .toss();
});