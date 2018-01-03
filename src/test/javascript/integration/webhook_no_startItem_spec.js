require('../integration_config');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var gUrl = utils.getWebhookUrl() + "/" + channelName;
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/' + channelName,
    batch: 'SINGLE',
    parallelCalls: 1,
    paused: false
}

describe(testName, function () {
    utils.createChannel(channelName, channelUrl, testName);
    utils.putWebhook(channelName, webhookConfig, 201, testName);

    utils.itSleeps(10000);
    
    it('gets webhook ' + channelName, function (done) {
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
                    if (typeof webhookConfig !== "undefined") {
                        console.log("lastCompleted: " + webhookConfig.lastCompleted);
                        var lastComp = JSON.parse(response.body).lastCompleted;
                        console.log("lastComp: " + lastComp);
                        expect(lastComp.indexOf("initial") > -1, true);
                        expect(parse.callbackUrl).toBe(webhookConfig.callbackUrl);
                        expect(parse.channelUrl).toBe(webhookConfig.channelUrl);
                        expect(parse.transactional).toBe(webhookConfig.transactional);
                        expect(parse.name).toBe(channelName);
                        expect(parse.batch).toBe(webhookConfig.batch);
                        if (webhookConfig.ttlMinutes) {
                            expect(parse.ttlMinutes).toBe(webhookConfig.ttlMinutes);
                        }
                        if (webhookConfig.maxWaitMinutes) {
                            expect(parse.maxWaitMinutes).toBe(webhookConfig.maxWaitMinutes);
                        }
                    }
                }
                done();
            });
    });

});

