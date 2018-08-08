require('../integration_config');
const { getProp } = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;

function verifyOptionals(parse) {
    expect(getProp('description', parse)).toBe('describe me');
    expect(getProp('ttlDays', parse)).toBe(9);
    expect(getProp('contentSizeKB', parse)).toBe(3);
    expect(getProp('peakRequestRateSeconds', parse)).toBe(2);
}

describe(testName, function () {
    it("fails to create channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                tags: ['one', 'tw*o']
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(400);
            done();
        });
    });

    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                tags: ['one', 'two']
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            var parse = utils.parseJson(response, testName);
            const tags = getProp('tags', parse);
            expect(tags).toContain('one');
            expect(tags).toContain('two');
            done();
        });
    });

    it("patches channel " + channelResource, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ tags: ['one', 'tw*o'] })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(400);
            done();
        });
    });
});
