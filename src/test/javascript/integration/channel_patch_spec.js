require('../integration_config');
const { getProp } = require('../lib/helpers');
var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;
var updateBody = {
    "ttlDays": 2,
    description: 'next',
    "tags": ["foo", "bar", "tagz"],
    replicationSource: 'http://hub/channel/nada'
};
function verifyOptionals(parse) {
    expect(getProp('description', parse)).toBe('describe me');
    expect(getProp('ttlDays', parse)).toBe(9);
    expect(getProp('replicationSource', parse)).toBe('');
}

describe(testName, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url: channelUrl,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                name: channelName,
                description: 'describe me',
                ttlDays: 9
            })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            var parse = utils.parseJson(response, testName + '_A');
            verifyOptionals(parse);
            done();
        });
    });

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parse = utils.parseJson(response, testName + '_B');
                verifyOptionals(parse);
                done();
            });
    });

    function verifyPatched(parse) {
        expect(getProp('ttlDays', parse)).toBe(2);
        expect(getProp('description', parse)).toBe('next');
        expect(getProp('replicationSource', parse)).toBe('http://hub/channel/nada');
        const tags = getProp('tags', parse);
        if (tags) {
            expect(tags).toContain('foo');
            expect(tags).toContain('bar');
            expect(tags).toContain('tagz');
        }
    }

    it("patches channel " + channelResource, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(updateBody)},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            var parse = utils.parseJson(response, testName + '_C');
            verifyPatched(parse);
            done();
        });
    });

    it("verifies channel exists with attributes " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parse = utils.parseJson(response, testName + '_D');
                verifyPatched(parse);
                done();
            });
    });

});
