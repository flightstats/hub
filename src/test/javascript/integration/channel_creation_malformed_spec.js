require('../integration_config');
const {  getProp } = require('../lib/helpers');

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
                expect(getProp('statusCode', response)).toBe(400);
                const responseBody = getProp('body', response);
                console.log(responseBody);
                expect(responseBody).toContain('xpect');
                done();
            });
    });
});
