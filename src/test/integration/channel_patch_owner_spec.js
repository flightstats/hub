require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(parse.owner).toBe('the man');
    expect(parse.ttlDays).toBe(9);
}

describe(testName, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({
                url: channelUrl,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    name: channelName,
                    owner: 'the man',
                    ttlDays: 9
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName + '_A');
                verifyOptionals(parse);
                done();
            });
    });

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName + '_B');
                verifyOptionals(parse);
                done();
            });
    });

    it("patches channel with null owner " + channelResource, function (done) {
        request.patch({
                url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({description: 'stuff'})
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName + '_C');
                expect(parse.description).toBe("stuff");
                done();
            });
    });

    it("verifies channel exists with correct owner " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName + '_D');
                expect(parse.description).toBe("stuff");
                done();
            });
    });

});

