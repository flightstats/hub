require('./../integration/integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var auditChannelResource = channelResource + '_audit';
var recursiveAuditChannel = auditChannelResource + '_audit';
var testName = __filename;
var user = 'nobody';
var foundAudits = [];

if (!runEncrypted) return;

/**
 * This should make sure that audited channels are not themselves audited.
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

    it("verifies audit channel does not have an audit channel " + recursiveAuditChannel, function (done) {
        request.get({url : recursiveAuditChannel },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });

});

