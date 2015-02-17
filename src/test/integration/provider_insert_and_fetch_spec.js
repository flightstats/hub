require('./../integration/integration_config.js');

var channelName = utils.randomChannelName();
var providerResource = hubUrlBase + "/provider";
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = "provider_insert_and_fetch_spec";

utils.configureFrisby();


frisby.create(testName + ': Inserting a value into a provider channel .')
    .post(providerResource, null, { body: messageText})
    .addHeader("channelName", channelName)
    .addHeader("Content-Type", "text/plain")
    .expectStatus(200)
    .expectHeader('content-type', 'text/plain')
    .after(function () {
        frisby.create(testName + ': Fetching value to ensure that it was inserted.')
            .get(thisChannelResource + "/latest?stable=false")
            .expectStatus(200)
            .expectHeader('content-type', 'text/plain')
            .expectBodyContains(messageText)
            .toss();
    })
    .toss();

