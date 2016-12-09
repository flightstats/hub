require('./integration_config.js');

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
                expect(response.statusCode).toBe(400);
                console.log(response.body);
                expect(response.body).toContain('xpect');
                done();
            });
    });
});