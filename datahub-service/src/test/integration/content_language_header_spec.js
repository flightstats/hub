require('./integration_config.js');
var request = require('request');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "Testing that the Content-Language header is returned";
var testName = "content_language_header_spec";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    // Note: We have to use request directly here, because Frisby insists on having a content-type specified.
    frisby.create(testName + " Testing the content-language header")
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Language", "en, sp")
        .expectStatus(201)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            console.info("yeah " + valueUrl);
            frisby.create(testName + ": Fetching to confirm header")
                .get(valueUrl)
                .expectHeader('content-language', 'en, sp')
                .toss()
        })
        .toss();
});