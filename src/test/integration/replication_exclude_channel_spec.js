require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var remoteUrl = "http://" + replicationDomain + "/channel";
var testName = "replication_exclude_channel_spec";

if (typeof replicationDomain === 'undefined') {
    xdescribe("replicationDomain is not defined, skipping replication_remote_spec", function () {
        console.info("replicationDomain is not defined, skipping replication_exclude_channel_spec");
        xit("is just a function, so it can contain any code", function () {
        });
    });
    return;
}
/**
 * This should exclude POSTs to a channel with replication configured and remote channel is available.
 * 1 - create local channel
 * 2 - create remote channel
 * 3 - configure replication with for channel
 * 4 - wait for replication of channel to start
 * 5 - attempt to POST to channel
 */
describe(testName, function () {
    utils.createChannel(channelName);
    utils.createChannel(channelName, remoteUrl);

    it("creates replication config", function (done) {
        request.put({url : hubUrlBase + "/replication/" + replicationDomain,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ historicalDays : 1, excludeExcept : [channelName] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    it("waits for channel replication to pick up", function () {
        var connected = false;

        function replicationCallback(body) {
            var statuses = JSON.parse(body)['status'];
            for (var i = 0; i < statuses.length; i++) {
                var status = statuses[i];
                if (status.name === channelName) {
                    connected = true;
                }
            }
            if (!connected) {
                getReplication(replicationCallback);
            }
        }

        runs(function () {
            getReplication(replicationCallback);
        });

        waitsFor(function () {
            return connected;
        }, 60000);

    }, 60000);

    utils.addItem(channelResource, 403);

    function getReplication(callback) {
        request.get({url : hubUrlBase + "/replication",
                headers : {"Content-Type" : "application/json"}},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                callback(body);
            });
    }
});
