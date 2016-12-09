require('./integration_config.js');

var request = require('request');
var http = require('http');
var webhookName = utils.randomChannelName();
var testName = __filename;

describe("creates malformed" + testName, function () {

    var webhookUrl = hubUrlBase + '/webhook';
    var webhookResource = webhookUrl + "/" + webhookName;

    it('creates webhook ' + webhookName, function (done) {
        console.log('creating webhook ' + webhookName + ' for ' + testName);
        request.put({
                url: webhookResource,
                headers: {"Content-Type": "application/json"},
                body: "{ callbackUrl : 'http://nothing/callback', channelUrl : 'http://nothing/channel/notHere' }"
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

