require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName + '/batch';
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

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
        '--abcdefg--'

    var items = [];

    it("batches items to " + channelResource, function (done) {
        request.post({
                url: channelResource,
                headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
                body: multipart
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = JSON.parse(response.body);
                console.log(response.body);
                expect(parse._links.uris.length).toBe(2);
                items = parse._links.uris;
                done();
            });
    });

    it("gets first item " + channelResource, function (done) {
        request.get({url: items[0]},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(response.body).toBe('<coffee><roast>french</roast><coffee>')
                expect(response.headers['content-type']).toBe('application/xml')
                done();
            });
    });

    it("gets second item " + channelResource, function (done) {
        request.get({url: items[1]},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(response.body).toBe('{ "type" : "coffee", "roast" : "french" }')
                expect(response.headers['content-type']).toBe('application/json')
                done();
            });
    });

});

