require('./integration_config.js');
var request = require('request');

var channelName = utils.randomChannelName();
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "Testing that the Content-Language header is returned";

utils.configureFrisby();

utils.runInTestChannel(channelName, function () {
    // Note: We have to use request directly here, because Frisby insists on having a content-type specified.
    frisby.create("Testing the content-language header")
        .post(thisChannelResource, null, { body: messageText})
        .addHeader("Content-Language", "en, sp")
        .expectStatus(201)
        .afterJSON(function (result) {
            var valueUrl = result['_links']['self']['href'];
            console.info("yeah " + valueUrl);
            frisby.create("Fetching to confirm header")
                .get(valueUrl)
                .expectHeader('content-language', 'en, sp')
                .toss()
        })
        .toss();
});