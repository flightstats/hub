require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var gUrl = groupUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();

describe(testName, function () {

    it("creates channel: " + channelName + " with group no initialTime.", function (done) {
        request.put({
                url: channelUrl + '/' + channelName,
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    maxItems: 1,
                    ttlDays: 1
                })
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(400);
                done();
            });

        request.put({
            url: gUrl,
            headers: {"Content-type": "application/json"},
            body: JSON.stringify({
                    callbackUrl: "http://fakey/bakey",
                    channelUrl: channelResource,
                    parallelCalls: 1,
                    paused: false,
                    batch: "SINGLE",
                    heartbeat: false,
                    maxWaitMinutes: 1,
                    ttlMinutes: 0
                },
                function (err, response, body) {
                    expect(err).toBe('');
                    //expect(response.statusCode).toBe(201);
                    done();
                })
        });

        console.log("Pulling the callback: " + gUrl);
        request.get({
                url: gUrl,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                console.log("crap");
                console.log("response: " + response);
                console.log("body: " + body);
            });
    });

});
