require('../integration_config');

var providerResource = hubUrlBase + "/provider/bulk";
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
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

describe(__filename, function () {

    it('inserts a bulk value into a provider channel', function (done) {
        var url = providerResource;
        var headers = {
            'channelName': channelName,
            'Content-Type': 'multipart/mixed; boundary=abcdefg'
        };
        var body = multipart;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
            })
            .finally(done);
    });

    it('verifies the bulk value was inserted', function (done) {
        var url = channelResource + '/latest?stable=false';

        utils.httpGet(url)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
            })
            .finally(done);
    });

});
