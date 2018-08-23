const { getProp, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const request = require('request');

describe(`creates malformed ${__filename}`, function () {
    it(`creates channel ${channelName}`, function (done) {
        console.log(`creating channel ${channelName} for ${__filename}`);
        request.put({
            url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: "{ description  'http://nothing/callback'  }",
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
