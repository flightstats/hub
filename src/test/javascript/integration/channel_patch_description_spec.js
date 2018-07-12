require('../integration_config');
const { getProp } = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(getProp('description', parse)).toBe('describe me');
    expect(getProp('ttlDays', parse)).toBe(9);
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
            var parse = utils.parseJson(response, testName);
            verifyOptionals(parse);
            done();
        });
    });

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parse = utils.parseJson(response, testName);
                verifyOptionals(parse);
                done();
            });
    });

    it("patches channel with null description " + channelResource, function (done) {
        request.patch({url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({ description: 'stuff' })},
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            var parse = utils.parseJson(response, testName);
            expect(getProp('description', parse)).toBe("stuff");
            done();
        });
    });

    it("verifies channel exists with correct description " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(getProp('description', parse)).toBe("stuff");
                done();
            });
    });

});
