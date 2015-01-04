require('./integration_config.js');
var request = require('request');
var testName = __filename;
var channelName = utils.randomChannelName();

/**
 * This test could be fragile relative to time.  If a remote server is running slowly, this may fail.
 */
describe(testName, function () {
    var channelName = utils.randomChannelName();
    utils.createChannel(channelName);

    var localChannelUrl = hubUrlBase + '/channel/' + channelName;
    var localReplicationUrl = hubUrlBase + '/replication/';

    it('creates local replication config', function (done) {
        request.put({url : localReplicationUrl + replicationDomain,
                headers : {'Content-Type' : 'application/json'},
                body : JSON.stringify({ historicalDays : 1, excludeExcept : [channelName] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    utils.sleep(1000);

    it('tries to delete channel', function (done) {
        request.del({url : localChannelUrl },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(403);
                done();
            });
    });


});
