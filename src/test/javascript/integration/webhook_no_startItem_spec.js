require('../integration_config');
const { createChannel, fromObjectPath, getProp } = require('../lib/helpers');
var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var gUrl = utils.getWebhookUrl() + "/" + channelName;
var testName = __filename;
var webhookConfig = {
    callbackUrl: 'http://nothing/callback',
    channelUrl: 'http://nothing/channel/' + channelName,
    batch: 'SINGLE',
    parallelCalls: 1,
    paused: false,
};
let createdChannel = false;

describe(testName, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, channelUrl, testName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    utils.putWebhook(channelName, webhookConfig, 201, testName);

    utils.itSleeps(10000);

    it('gets webhook ' + channelName, function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        request.get({
            url: gUrl,
            headers: {"Content-Type": "application/json"},
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);

            if (response.statusCode < 400) {
                var parse = utils.parseJson(response, channelName);
                const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
                expect(selfLink).toBe(gUrl);
                if (typeof webhookConfig !== "undefined") {
                    console.log("lastCompleted: " + webhookConfig.lastCompleted);
                    let lastComp = '';
                    try {
                        const body = getProp('body', response);
                        const bodyParse = JSON.parse(body);
                        lastComp = getProp('lastCompleted', bodyParse) || '';
                    } catch (ex) {
                        console.log(`error parsing response body, ${ex}`);
                    }
                    console.log("lastComp: " + lastComp);
                    expect((lastComp || '').indexOf("initial") > -1, true);
                    expect(getProp('callbackUrl', parse)).toBe(webhookConfig.callbackUrl);
                    expect(getProp('channelUrl', parse)).toBe(webhookConfig.channelUrl);
                    expect(getProp('transactional', parse)).toBe(null);
                    expect(getProp('name', parse)).toBe(channelName);
                    expect(getProp('batch', parse)).toBe(webhookConfig.batch);
                    if (webhookConfig.ttlMinutes) {
                        expect(getProp('ttlMinutes', parse)).toBe(webhookConfig.ttlMinutes);
                    }
                    if (webhookConfig.maxWaitMinutes) {
                        expect(getProp('maxWaitMinutes', parse)).toBe(webhookConfig.maxWaitMinutes);
                    }
                }
            }
            done();
        });
    });
});
