require('./integration_config.js');
var request = require('request');

if(typeof remoteDomain === 'undefined'){
    xdescribe("remoteDomain is not defined, skipping replication_remote_spec", function() {
        console.info("remoteDomain is not defined, skipping replication_remote_spec");
        xit("is just a function, so it can contain any code", function() { });
    });
    return;
}

/**
 * This test could be fragile relative to time.  If a remote server is running slowly, this may fail.
 */
describe("replication_remote_spec", function () {

    var channelName = utils.randomChannelName();
    var remoteUrl = "http://" + remoteDomain + "/channel";
    it("creates remote channel", function (done) {
        request.post({url: remoteUrl,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ "name": channelName, "description": "re-moat channel"})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    it("adds remote items", function (done) {
        addItem();
        addItem(done);
    });

    it("creates local replication config", function (done) {
        request.put({url: hubUrlBase + "/replication/" + remoteDomain,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ historicalDays: 1, excludeExcept: [channelName] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    it("tries to replicate duplicate channel", function (done) {
        request.put({url: hubUrlBase + "/replication/duper" ,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ historicalDays: 1, excludeExcept: [channelName] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(403);
                done();
            });
    });

    it("waits for connection to remote server", function () {
        var connected = false;

        function replicationCallback(body) {
            var statuses = JSON.parse(body)['status'];
            for (var i = 0; i < statuses.length; i++) {
                var status = statuses[i];
                if (status.name === channelName && status.connected === true) {
                    connected = true;
                }
            }
            if (!connected) {
                getReplication(replicationCallback);
            }
        }

        runs(function() {
            getReplication(replicationCallback);
        });

        waitsFor(function() {
            return connected;
        }, 60000);

    });

    it("adds more remote items", function (done) {
        addItem();
        addItem(done);
    });

    it("verifies replication progress", function () {
        var verified = false;

        function replicationCallback(body) {
            var status = JSON.parse(body)['status'][0];
            if (status['name'] === channelName && status['replicationLatest'] === 1003) {
                verified = true;
            } else {
                getReplication(replicationCallback);
            }
        }

        runs(function() {
            getReplication(replicationCallback);
        });

        waitsFor(function() {
            return verified;
        }, 60000);

    });



    it("verifies local copy of the channel", function() {
        var verified = false;

        runs(function() {
            request.get({url: hubUrlBase + "/channel/" + channelName,
                    headers: {"Content-Type": "application/json"}},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    jsonBody = JSON.parse(body);
                    expect(jsonBody['description']).toBe("re-moat channel");
                    verified = true;
                });
        });

        waitsFor(function() {
            return verified;
        }, 10000);

    });

    function getReplication(callback) {
        request.get({url: hubUrlBase + "/replication",
                headers: {"Content-Type": "application/json"}},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                callback(body);
            });
    }

    function addItem(done) {
        done = done || function () { };
        request.post({url: remoteUrl + "/" + channelName,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({ "data": Date.now()})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    }

});
