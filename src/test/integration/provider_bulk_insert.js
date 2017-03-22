require('./../integration/integration_config.js');

utils.configureFrisby();
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

var providerBulkResource = hubUrlBase + "/provider/bulk";
bulkTestName = "provider_bulk_insert_and_fetch_spec";
bulkChannelName = utils.randomChannelName();
bulkChannelResource = channelUrl + "/" + bulkChannelName;
frisby.create(bulkTestName + ': Inserting a bulk value into a provider channel .')
    .post(providerBulkResource, null, { body: multipart})
    .addHeader("channelName", bulkChannelName)
    .addHeader("Content-Type", "multipart/mixed; boundary=abcdefg")
    .expectStatus(200)
    .after(function () {
        frisby.create(bulkTestName + ': Fetching bulk value to ensure that it was inserted.')
            .get(bulkChannelResource + "/latest?stable=false")
            .expectStatus(200)
            .toss();
    })
    .toss();