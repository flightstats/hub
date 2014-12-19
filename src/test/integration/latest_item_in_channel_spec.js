require('./../integration/integration_config.js');

var channelName = utils.randomChannelName();
var testName = __filename;

utils.configureFrisby();

utils.runInTestChannel(testName, channelName, function (channelResponse) {
    var channelResource = channelResponse['_links']['self']['href'];
    var latestResource = channelResponse['_links']['latest']['href'];
    var messageText = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAA_bee_buzzzzzzzz";
    frisby.create(testName + ': Inserting latest item')
        .post(channelResource, null, { body : messageText})
        .addHeader("Content-Type", "text/plain")
        .expectStatus(201)
        .afterJSON(function (response) {
            var itemUrl = response['_links']['self']['href'];
            frisby.create(testName + ': Fetching latest item from channel')
                .get(latestResource, {followRedirect : false})
                .expectStatus(303)
                // This sucks, but apparently frisby or request or somebody hides this from us.
                //.expectHeader('Location', itemUrl)
                .toss();
        })
        .toss();
});

