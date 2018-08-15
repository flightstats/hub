require('../integration_config');
const { getProp, getWebhookUrl } = require('../lib/helpers');
var request = require('request');
var webhookName = utils.randomChannelName();
var webhookResource = `${getWebhookUrl()}/${webhookName}`;
var testName = __filename;

describe(testName, function () {
    it('gets missing webhook ' + webhookName, function (done) {
        request.get({
            url: webhookResource,
            headers: { "Content-Type": "application/json" } },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(404);
            done();
        });
    });
});
