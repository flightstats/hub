require('./../integration/integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var auditChannelResource = channelResource + '_audit';
var testName = __filename;

if (!runEncrypted) return;

/**
 * This should make sure we can't delete an auditing channel
 */
describe(testName, function () {
    utils.createChannel(channelName);

    it("fails to delete audit channel  " + auditChannelResource, function (done) {
        request.del({url : auditChannelResource },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(403);
                done();
            });
    });

});

