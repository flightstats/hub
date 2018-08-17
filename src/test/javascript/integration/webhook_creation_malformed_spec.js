require('../integration_config');
const request = require('request');
const { getProp } = require('../lib/helpers');
const {
    getHubUrlBase,
} = require('../lib/config');

const webhookName = utils.randomChannelName();
const webhookUrl = `${getHubUrlBase()}/webhook`;
const webhookResource = `${webhookUrl}/${webhookName}`;

describe(`creates malformed${__filename}`, function () {
    it(`creates webhook ${webhookName}`, function (done) {
        console.log(`creating webhook ${webhookName} for ${__filename}`);
        request.put({
            url: webhookResource,
            headers: {"Content-Type": "application/json"},
            body: "{ callbackUrl : 'http://nothing/callback', channelUrl : 'http://nothing/channel/notHere' }",
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
