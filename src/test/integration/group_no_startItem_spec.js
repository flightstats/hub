require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var gUrl = groupUrl + "/" + channelName;
var testName = __filename;
var groupConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/' + channelName,
    batch: 'SINGLE',
    parallelCalls: 1,
    paused: false
}

describe(testName, function () {
    utils.createChannel(channelName, channelUrl, testName);
    utils.putGroup(channelName, groupConfig, 201, testName);

    it('gets group ' + channelName, function (done) {
        utils.sleep(10000);
        request.get({
                url: gUrl,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);

                if (response.statusCode < 400) {
                    var parse = utils.parseJson(response, channelName);
                    expect(parse._links.self.href).toBe(gUrl);
                    if (typeof groupConfig !== "undefined") {
                        console.log("lastCompletedCallback: " + groupConfig.lastCompletedCallback);
                        var lastComp = JSON.parse(response.body).lastCompletedCallback;
                        console.log("lastComp: " + lastComp);
                        expect(lastComp.indexOf("initial") > -1, true);
                        expect(parse.callbackUrl).toBe(groupConfig.callbackUrl);
                        expect(parse.channelUrl).toBe(groupConfig.channelUrl);
                        expect(parse.transactional).toBe(groupConfig.transactional);
                        expect(parse.name).toBe(channelName);
                        expect(parse.batch).toBe(groupConfig.batch);
                        if (groupConfig.ttlMinutes) {
                            expect(parse.ttlMinutes).toBe(groupConfig.ttlMinutes);
                        }
                        if (groupConfig.maxWaitMinutes) {
                            expect(parse.maxWaitMinutes).toBe(groupConfig.maxWaitMinutes);
                        }
                    }
                }
                done();
            });
    });

});

