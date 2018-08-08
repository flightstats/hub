require('../integration_config');
const { getProp } = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;

describe(testName, function () {

    it("puts channel with ttl and max " + channelName, function (done) {
        request.put({
                url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    maxItems: 1,
                    ttlDays: 1
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(400);
                done();
            });
    });

});
