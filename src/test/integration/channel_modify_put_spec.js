require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(parse.owner).toBe('the man');
    expect(parse.description).toBe('describe');
    expect(parse.ttlDays).toBe(9);
    expect(parse.tags).toContain('bar');
    expect(parse.tags).toContain('foo-bar');

}

describe(testName, function () {

    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.put({
                url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    name: channelName,
                    description: 'describe',
                    owner: 'the man',
                    ttlDays: 9,
                    tags: ['bar', 'foo-bar']
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
                verifyOptionals(parse);
                done();
            });
    });

    it("put channel with empty json" + channelResource, function (done) {
        request.put({
                url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({})
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
                verifyOptionals(parse);
                done();
            });
    });

    it("put channel with no payload" + channelResource, function (done) {
        request.put({
                url: channelResource
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
                verifyOptionals(parse);
                done();
            });
    });

});

