const request = require('request');
const { getProp, getWebhookUrl, randomChannelName } = require('../lib/helpers');

const webhookName = randomChannelName();
const webhookResource = `${getWebhookUrl()}/${webhookName}`;

describe(__filename, function () {
    it(`gets missing webhook ${webhookName}`, function (done) {
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
