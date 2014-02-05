require('./integration_config.js');
var request = require('request');

var testName = "replication_remote_spec";

describe("replication suite", function () {

    it("doing all replication", function () {

        var channelName = utils.randomChannelName();

        var jsonBody = JSON.stringify({
            historicalDays: 1,
            excludeExcept: [channelName]
        });
        var remoteUrl = "http://" + remoteDomain + "/channel";
        var replicationResource = hubUrlBase + "/replication/" + remoteDomain;

        var createdChannels = false;
        var createdReplication = false;
        var gotReplication = false;

        runs(function () {
            createChannel();
        });

        waitsFor(function () {
            return createdChannels;
        }, 15000);

        runs(function () {
            //start replication process
            request.put({url: replicationResource,
                    headers: {"Content-Type": "application/json"},
                    body: jsonBody},
                function (err, response, body) {
                    //todo verify
                    expect(err).toBeNull();
                    console.info("set up replication");
                    expect(response.statusCode).toBe(201);
                    createdReplication = true;
                });
        });

        waitsFor(function () {
            return createdReplication;
        }, 15000);

        runs(function () {
            addData("one");
            addData("two");
        });

        waits(5000);

        runs(function () {
            addData("three");
            addData("four");
        });

        waits(5000);

        runs(function () {
            addData("five");
            addData("six");
        });

        waits(5000);

        runs(function() {
            getReplication();
        })

        waitsFor(function () {
            return gotReplication;
        }, 10000)



        function getReplication() {
            request.get({url: hubUrlBase + "/replication",
                    headers: {"Content-Type": "application/json"} },
                function (err, response, body) {
                    expect(err).toBeNull();
                    console.info("get repl " + response.statusCode + " " + body);
                    expect(response.statusCode).toBe(200);
                    resultObj = JSON.parse(body);
                    expect(resultObj['status'][0]['name']).toBe(channelName);
                    expect(resultObj['status'][0]['replicationLatest']).toBe(1005);
                    gotReplication = true;
                });
        }

        function createChannel() {
            console.info("creating channel " + channelName + " at " + remoteUrl);
            request.post({url: remoteUrl,
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({ "name": channelName})},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(201);
                    console.info("creating channel " + remoteUrl + "/" + channelName + " response " + response.statusCode);
                    createdChannels = true;
                });

        }

        function addData(data) {
            request.post({url: remoteUrl + "/" + channelName,
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({ "data": data})},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(201);
                    console.info("added data to channel " + channelName + " " + data);
                });
        }
    });

});
