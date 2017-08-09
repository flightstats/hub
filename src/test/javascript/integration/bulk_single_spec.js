require('../integration_config');

var request = require('request');
var Q = require('q');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;


/**
 * create a channel
 * post 4 items as multipart to the single item endpoint.
 */
describe(testName, function () {

    utils.putChannel(channelName, false, {"name": channelName, "ttlDays": 1, "tags": ["bulk"]}, testName);

    var multipart =
        'This is a message with multiple parts in MIME format.  This section is ignored.\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message one\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message two\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message three\r\n' +
        '--abcdefg\r\n' +
        'Content-Type: text/plain\r\n' +
        ' \r\n' +
        'message four\r\n' +
        '--abcdefg--'

    var items = [];
    var location = '';

    it("post multipart item to " + channelName, function (done) {
        request.post({
                url: channelResource,
                headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
                body: multipart
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                location = response.headers.location;
                console.log('location', location);
                expect(response.headers.location).toBeDefined();
                var parse = utils.parseJson(response, testName);
                console.log(response.body);
                done();
            });
    });

    it("verifies content " + channelName, function (done) {
        request.get({url: location},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log(body);
                console.log(response.headers);
                expect(response.headers['content-type']).toBe('multipart/mixed;boundary=abcdefg');
                expect(body).toBe(multipart);
                done();
            });
    });

});
