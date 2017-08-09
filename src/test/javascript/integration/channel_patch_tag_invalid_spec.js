require('../integration_config');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(parse.description).toBe('describe me');
    expect(parse.ttlDays).toBe(9);
    expect(parse.contentSizeKB).toBe(3);
    expect(parse.peakRequestRateSeconds).toBe(2);
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
                expect(response.statusCode).toBe(400);
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
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
                expect(parse.tags).toContain('one');
                expect(parse.tags).toContain('two');
                done();
            });
    });

    it("patches channel " + channelResource, function (done) {
        request.patch({url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ tags: ['one', 'tw*o'] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(400);
                done();
            });
    });
});

