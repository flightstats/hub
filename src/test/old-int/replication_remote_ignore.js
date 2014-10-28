require('./../integration/integration_config.js');
var request = require('request');

if (typeof replicationDomain === 'undefined') {
    xdescribe("replicationDomain is not defined, skipping replication_remote_spec", function () {
        console.info("replicationDomain is not defined, skipping replication_remote_spec");
        xit("is just a function, so it can contain any code", function () {
        });
    });
    return;
}

/**
 * This test could be fragile relative to time.  If a remote server is running slowly, this may fail.
 */
describe("replication_remote_spec", function () {

    var channelName = utils.randomChannelName();
    var remoteChannelUrl = "http://" + replicationDomain + "/channel";
    var localChannelUrl = hubUrlBase + "/channel";
    var localReplicationUrl = hubUrlBase + "/replication/";
    it("creates remote channel", function (done) {
        request.post({url : remoteChannelUrl,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ "name" : channelName, "description" : "re-moat channel"})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    it("adds remote items", function (done) {
        addItem(remoteChannelUrl);
        addItem(remoteChannelUrl, done);
    });

    it("creates local replication config", function (done) {
        request.put({url : localReplicationUrl + replicationDomain,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ historicalDays : 1, excludeExcept : [channelName] })},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    });

    it("tries to replicate duplicate channel", function (done) {
        request.put({url : localReplicationUrl + "duper",
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ historicalDays : 1, excludeExcept : [channelName] })},
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

        runs(function () {
            getReplication(replicationCallback);
        });

        waitsFor(function () {
            return connected;
        }, 60000);

    });

    it("adds more remote items", function (done) {
        addItem(remoteChannelUrl);
        addItem(remoteChannelUrl, done);
    });

    it("verifies replication progress", function () {
        var verified = false;

        function replicationCallback(body) {
            var allStatus = JSON.parse(body)['status'];
            allStatus.forEach(function (channel) {
                if (channel['name'] === channelName && channel['replicationLatest'] === 1003) {
                    verified = true;
                } else {
                    getReplication(replicationCallback);
                }
            });
        }

        runs(function () {
            getReplication(replicationCallback);
        });

        waitsFor(function () {
            return verified;
        }, 60000);

    });

    it("verifies local copy of the channel", function () {
        var verified = false;

        runs(function () {
            request.get({url : localChannelUrl + "/" + channelName,
                    headers : {"Content-Type" : "application/json"}},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    jsonBody = JSON.parse(body);
                    expect(jsonBody['description']).toBe("re-moat channel");
                    verified = true;
                });
        });

        waitsFor(function () {
            return verified;
        }, 'waiting for verified ' + testName, 30000);

    });

    it("stops replication ", function (done) {
        request.del({url : localReplicationUrl + replicationDomain },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(202);

                done();
            });
    });


    it("adds local items to " + channelName, function (done) {
        var channelUrl = localChannelUrl + "/" + channelName;
        request.get({url : channelUrl + "/1003"},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
            });
        addItem(localChannelUrl);
        addItem(localChannelUrl, done);
    });

    it("verifies last item in " + channelName, function (done) {
        var channelUrl = localChannelUrl + "/" + channelName;
        request.get({url : channelUrl + "/1005"},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    });

    it("verifies latest item in " + channelName, function (done) {
        var channelUrl = localChannelUrl + "/" + channelName;
        request.get({url : channelUrl + "/latest", followRedirect : false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(channelUrl + "/1005");
                done();
            });
    });


    function getReplication(callback) {
        request.get({url : localReplicationUrl,
                headers : {"Content-Type" : "application/json"}},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                callback(body);
            });
    }

    function addItem(url, done) {
        done = done || function () {
        };
        request.post({url : url + "/" + channelName,
                headers : {"Content-Type" : "application/json"},
                body : JSON.stringify({ "data" : Date.now()})},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                done();
            });
    }

});
