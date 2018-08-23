const { getProp, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it(`puts channel with ttl and max ${channelName}`, function (done) {
        request.put({
            url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                maxItems: 1,
                ttlDays: 1,
            }),
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(400);
            done();
        });
    });
});
