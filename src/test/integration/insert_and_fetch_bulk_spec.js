require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName + '/bulk';
var testName = __filename;

describe(testName, function () {
    utils.putChannel(channelName, false, {"ttlDays": 1, "tags": ["bulk"]}, testName);

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

    var items = [];

    it("bulk items to " + channelResource, function (done) {
        request.post({
                url: channelResource,
                headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
                body: multipart
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
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
                expect(response.body).toBe('<coffee><roast>french</roast><coffee>');
                expect(response.headers['content-type']).toBe('application/xml');
                done();
            });
    });

    it("gets second item " + channelResource, function (done) {
        request.get({url: items[1]},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(response.body).toBe('{ "type" : "coffee", "roast" : "french" }');
                expect(response.headers['content-type']).toBe('application/json');
                done();
            });
    });

    it("calls previous " + channelResource, function (done) {
        request.get({url: items[1] + '/previous?trace=true&stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                //console.log('response', response);
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(items[0]);
                done();
            });
    });

    it("calls next " + channelResource, function (done) {
        request.get({url: items[0] + '/next?trace=true&stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                console.log('body', body);
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(items[1]);
                done();
            });
    });
});

