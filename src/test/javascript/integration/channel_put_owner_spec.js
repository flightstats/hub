require('../integration_config');
const { getProp } = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(getProp('owner', parse)).toBe('the man');
}

describe(testName, function () {
    console.log('channel url', channelResource);
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.put({
            url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                owner: 'the man'
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

    it("puts channel with null owner " + channelResource, function (done) {
        request.put({
            url: channelResource,
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({owner: 'stuff'})
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            var parse = utils.parseJson(response, testName);
            expect(getProp('owner', parse)).toBe("stuff");
            done();
        });
    });

    it("verifies channel exists with correct owner " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(getProp('owner', parse)).toBe("stuff");
                done();
            });
    });

});
