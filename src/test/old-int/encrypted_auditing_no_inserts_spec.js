require('./../integration/integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var auditChannelResource = channelResource + '_audit';
var testName = __filename;

if (!runEncrypted) return;

/**
 * This should make sure we can't insert into an auditing channel
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

    utils.addItem(auditChannelResource, 403);

});

