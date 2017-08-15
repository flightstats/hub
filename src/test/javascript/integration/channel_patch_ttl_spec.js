require('../integration_config');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(parse.description).toBe('describe me');
    expect(parse.tags).toContain('one');
    expect(parse.tags).toContain('two');
}

describe(testName, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url : channelUrl,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({
                    name : channelName,
                    description : 'describe me',
                    ttlDays : 10,
                    contentSizeKB : 3,
                    peakRequestRateSeconds : 2,
                    tags : ['one', 'two']
                })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
                expect(parse.ttlDays).toBe(10);
                done();
            });
    });

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(parse.ttlDays).toBe(10);
                verifyOptionals(parse);
                done();
            });
    });

    it("patches channel " + channelResource, function (done) {
        request.patch({url : channelResource,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ ttlDays : 12 })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(parse.ttlDays).toBe(12);
                verifyOptionals(parse);
                done();
            });
    });

    it("verifies channel exists with correct ttl " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(parse.ttlDays).toBe(12);
                verifyOptionals(parse);
                done();
            });
    });

});

