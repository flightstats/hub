require('../integration_config');
const request = require('request');
const { getProp } = require('../lib/helpers');
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
            expect(getProp('statusCode', response)).toBe(400);
            console.log(getProp('body', response));
            expect(getProp('body', response)).toContain('xpect');
            done();
        });
    });

});
