require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var remoteUrl = "http://" + replicationDomain + "/channel";
var testName = __filename;

if (typeof replicationDomain === 'undefined') {
    xdescribe("replicationDomain is not defined, skipping " + testName, function () {
        console.info("replicationDomain is not defined, skipping " + testName);
        xit("is just a function, so it can contain any code", function () {
        });
    });
    return;
}
/**
 *
 * 1 - create local channel
 * 2 - create remote channel
 * 3 - configure replication with for channel using the new channel-specific api
 */
describe(testName, function () {
    utils.createChannel(channelName);
    utils.createChannel(channelName, remoteUrl);

    it("creates replication config", function (done) {
        request.put({
                url: hubUrlBase + "/replication/" + replicationDomain + '/' + channelName,
                headers: {"Content-Type": "application/json"}
            },
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
        }, 10000);

    });

    function getReplication(callback) {
        request.get({
                url: hubUrlBase + "/replication",
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                callback(body);
            });
    }
});
