require('../integration_config');
const { createChannel, fromObjectPath, getProp, getWebhookUrl } = require('../lib/helpers');
const request = require('request');
const channelName = utils.randomChannelName();
const gUrl = `${getWebhookUrl()}/${channelName}`;
const testName = __filename;
const webhookConfig = {
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
            const statusCode = getProp('statusCode', response);
            expect(statusCode).toBe(200);

            if (statusCode < 400) {
                const parse = utils.parseJson(response, channelName);
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
