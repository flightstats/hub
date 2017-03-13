require('./../integration/integration_config.js');

var channelName = utils.randomChannelName();
var providerResource = hubUrlBase + "/provider";
var providerBulkResource = hubUrlBase + "/provider/bulk";
var thisChannelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();
var testName = "provider_insert_and_fetch_spec";

utils.configureFrisby();


frisby.create(testName + ': Inserting a value into a provider channel .')
    .post(providerResource, null, { body: messageText})
    .addHeader("channelName", channelName)
    .addHeader("Content-Type", "text/plain")
    .expectStatus(200)
    .after(function () {
        frisby.create(testName + ': Fetching value to ensure that it was inserted.')
            .get(thisChannelResource + "/latest?stable=false")
            .expectStatus(200)
            .expectHeader('content-type', 'text/plain')
            .expectBodyContains(messageText)
            .toss();
    })
    .toss();

var multipart =
    'This is a message with multiple parts in MIME format.  This section is ignored.\r\n' +
    '--abcdefg\r\n' +
    'Content-Type: application/xml\r\n' +
    ' \r\n' +
    '<coffee><roast>french</roast><coffee>\r\n' +
    '--abcdefg\r\n' +
    'Content-Type: application/json\r\n' +
    ' \r\n' +
    '{ "type" : "coffee", "roast" : "french" }\r\n' +
    '--abcdefg--';

frisby.create(testName + ': Inserting a bulk into a provider channel .')
    .post(providerResource, null, { body: multipart})
    .addHeader("channelName", channelName)
    .addHeader("Content-Type", "multipart/mixed; boundary=abcdefg")
    .expectStatus(200)
    .after(function () {
        frisby.create(testName + ': Fetching value to ensure that it was inserted.')
            .get(thisChannelResource + "/latest?stable=false")
            .expectStatus(200)
            .expectHeader('content-type', 'multipart/mixed')
            .expectBodyContains(messageText)
            .toss();
    })
    .toss();