require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

function verifyOptionals(parse) {
    expect(parse.description).toBe('describe me');
    expect(parse.ttlDays).toBe(9);
}

describe(testName, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url : channelUrl,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({
                    name : channelName,
                    description : 'describe me',
                    ttlDays : 9,
                    contentSizeKB : 3,
                    peakRequestRateSeconds : 2,
                    tags : ['one', 'two']
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

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url : channelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                expect(parse.tags).toContain('one');
                expect(parse.tags).toContain('two');
                verifyOptionals(parse);
                done();
            });
    });

    function verifyPatched(parse) {
        if (parse.tags) {
            expect(parse.tags).toContain('one');
            expect(parse.tags).toContain('three');
            expect(parse.tags).not.toContain('two');
        }
        verifyOptionals(parse);
    }

    it("patches channel " + channelResource, function (done) {
        request.patch({url : channelResource,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ tags : ['one', 'three'] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                verifyPatched(parse);
                done();
            });
    });

    it("verifies channel exists with correct tags " + channelResource, function (done) {
        request.get({url : channelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                verifyPatched(parse);
                done();
            });
    });

});

