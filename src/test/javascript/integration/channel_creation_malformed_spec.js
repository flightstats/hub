require('../integration_config');
const { getResponseBody, getStatusCode } = require('../lib/helpers');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var testName = __filename;

describe("creates malformed" + testName, function () {

    var channelUrl = hubUrlBase + '/channel';
    var channelResource = channelUrl + "/" + channelName;

    it('creates channel ' + channelName, function (done) {
        console.log('creating channel ' + channelName + ' for ' + testName);
        request.put({
                url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: "{ description  'http://nothing/callback'  }"
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getStatusCode(response)).toBe(400);
                const responseBody = getResponseBody(response);
                console.log(responseBody);
                expect(responseBody).toContain('xpect');
                done();
            });
    });
});
