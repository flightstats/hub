require('./../integration/integration_config.js');
var request = require('request');

/**
 * This test could be fragile relative to time.  If a remote server is running slowly, this may fail.
 */
describe("replication_self_spec", function () {

    var channelName = utils.randomChannelName();

    it("creates local replication config " + hubUrlBase, function (done) {
        request.put({url : hubUrlBase + "/replication/" + hubDomain,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ historicalDays : 1, excludeExcept : [channelName] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(400);
                done();
            });
    });


});
