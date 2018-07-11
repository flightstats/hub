require('../integration_config');
const { getStatusCode } = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
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
                expect(getStatusCode(response)).toBe(400);
                done();
            });
    });

});
