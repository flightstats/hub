require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var updateBody = {
    "ttlDays" : 2,
    description: 'next',
    "tags": ["foo", "bar", "tagz"],
    replicationSource: 'http://hub/channel/nada'
};
function verifyOptionals(parse) {
    expect(parse.description).toBe('describe me');
    expect(parse.ttlDays).toBe(9);
    expect(parse.replicationSource).toBe('');
}

describe(testName, function () {
    it("creates channel " + channelName + " at " + channelUrl, function (done) {
        request.post({url : channelUrl,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({
                    name : channelName,
                    description : 'describe me',
                    ttlDays : 9
                })},
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

    function verifyPatched(parse) {
        expect(parse.ttlDays).toBe(2);
        expect(parse.description).toBe('next');
        expect(parse.replicationSource).toBe('http://hub/channel/nada');
        if (parse.tags) {
            expect(parse.tags).toContain('foo');
            expect(parse.tags).toContain('bar');
            expect(parse.tags).toContain('tagz');
        }
    }

    it("patches channel " + channelResource, function (done) {
        request.patch({url : channelResource,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify(updateBody)},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName + '_C');
                verifyPatched(parse);
                done();
            });
    });

    it("verifies channel exists with attributes " + channelResource, function (done) {
        request.get({url: channelResource + '?cached=false'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName + '_D');
                verifyPatched(parse);
                done();
            });
    });

});

