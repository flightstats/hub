require('./../integration/integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var auditChannelResource = channelResource + '_audit';
var testName = __filename;

if (!runEncrypted) return;

/**
 * This should make sure that audited channels still have a audit tag when patched
 */
describe(testName, function () {
    utils.createChannel(channelName);

    it("verifies audit channel exists " + auditChannelResource, function (done) {
        request.get({url : auditChannelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    });

    it("patches audit channel" + auditChannelResource, function (done) {
        request.patch({url : auditChannelResource,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ tags : ['stuff'] })},
            function (err, response, body) {
                expect(err).toBeNull();
                var parse = JSON.parse(body);
                expect(response.statusCode).toBe(200);
                expect(parse.tags).toContain('audit');
                expect(parse.tags).toContain('stuff');
                done();
            });
    });

    it("verifies audit channel exists with tag " + auditChannelResource, function (done) {
        request.get({url : auditChannelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = JSON.parse(body);
                expect(parse.name).toBe(channelName + '_audit');
                expect(parse.tags).toContain('audit');
                expect(parse.tags).toContain('stuff');
                done();
            });
    });

});

