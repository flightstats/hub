require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url: channelUrl,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ "name": channelName, tags: ['one', 'two'] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = JSON.parse(body);
                expect(parse.tags).toContain('one');
                expect(parse.tags).toContain('two');
                done();
            });
    });

    it("verifies channel exists " + channelResource, function (done) {
        request.get({url: channelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse.tags).toContain('one');
                expect(parse.tags).toContain('two');
                done();
            });
    });

    it("patches channel " + channelResource, function (done) {
        request.patch({url: channelResource,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ tags: ['one', 'three'] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse.tags).toContain('one');
                expect(parse.tags).toContain('three');
                expect(parse.tags).not.toContain('two');
                done();
            });
    });

    it("verifies channel exists with correct tags " + channelResource, function (done) {
        request.get({url: channelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse.tags).toContain('one');
                expect(parse.tags).toContain('three');
                expect(parse.tags).not.toContain('two');
                done();
            });
    });

});

