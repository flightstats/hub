require('../integration_config');
const { getProp } = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(getProp('owner', parse)).toBe('the man');
    expect(getProp('description', parse)).toBe('describe');
    expect(getProp('ttlDays', parse)).toBe(9);
    expect(getProp('tags', parse)).toContain('bar');
    expect(getProp('tags', parse)).toContain('foo-bar');

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
            expect(getProp('statusCode', response)).toBe(201);
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
            expect(getProp('statusCode', response)).toBe(201);
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
            expect(getProp('statusCode', response)).toBe(201);
            var parse = utils.parseJson(response, testName);
            verifyOptionals(parse);
            done();
        });
    });

});
