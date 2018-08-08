require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
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
            expect(getProp('statusCode', response)).toBe(201);
            var parse = utils.parseJson(response, testName);
            console.log(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], parse) || [];
            expect(uris.length).toBe(2);
            items = uris;
            done();
        });
    });

    it("gets first item " + channelResource, function (done) {
        request.get({url: items[0]},
            function (err, response, body) {
                expect(err).toBeNull();
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(getProp('statusCode', response)).toBe(200);
                expect(getProp('body', response)).toBe('<coffee><roast>french</roast><coffee>');
                expect(contentType).toBe('application/xml');
                done();
            });
    });

    it("gets second item " + channelResource, function (done) {
        request.get({url: items[1]},
            function (err, response, body) {
                expect(err).toBeNull();
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                expect(getProp('statusCode', response)).toBe(200);
                expect(getProp('body', response)).toBe('{ "type" : "coffee", "roast" : "french" }');
                expect(contentType).toBe('application/json');
                done();
            });
    });

    it("calls previous " + channelResource, function (done) {
        request.get({url: items[1] + '/previous?trace=true&stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                const location = fromObjectPath(['headers', 'location'], response);
                // console.log('response', response);
                expect(getProp('statusCode', response)).toBe(303);
                expect(location).toBe(items[0]);
                done();
            });
    });

    it("calls next " + channelResource, function (done) {
        request.get({url: items[0] + '/next?trace=true&stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                console.log('body', body);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(getProp('statusCode', response)).toBe(303);
                expect(location).toBe(items[1]);
                done();
            });
    });
});
