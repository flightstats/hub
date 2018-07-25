require('../integration_config');
const { createChannel, fromObjectPath, getProp } = require('../lib/helpers');
var request = require('request');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
};
let createdChannel = false;
/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 */
describe(testName, function () {
    var callbackServer;
    var callbackItems = [];
    var postedItems = [];

    beforeAll(async () => {
        const channel = await createChannel(channelName, false, testName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    it('starts a callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            callbackItems.push(string);
        }, done);
    });

    it('inserts items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('closes the first callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        for (var i = 0; i < callbackItems.length; i++) {
            let parse = {};
            try {
                parse = JSON.parse(callbackItems[i]);
            } catch (ex) {
                expect(`failed to parse json, ${callbackItems[i]}, ${ex}`).toBeNull();
            }
            const uris = getProp('uris', parse) || [];
            const name = getProp('name', parse);
            expect(uris[0]).toBe(postedItems[i]);
            expect(name).toBe(webhookName);
        }
    });

    it('verifies lastCompleted', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        var webhookResource = utils.getWebhookUrl() + "/" + webhookName;
        request.get({
            url: webhookResource,
            headers: { "Content-Type": "application/json" } },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            var parse = utils.parseJson(response, testName);
            const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
            expect(selfLink).toBe(webhookResource);
            if (typeof webhookConfig !== "undefined") {
                expect(getProp('callbackUrl', parse)).toBe(webhookConfig.callbackUrl);
                expect(getProp('channelUrl', parse)).toBe(webhookConfig.channelUrl);
                // expect(getProp('transactional', parse)).toBe(webhookConfig.transactional);
                expect(getProp('name', parse)).toBe(webhookName);
                expect(getProp('lastCompleted', parse)).toBe(postedItems[3]);
            }
            done();
        });
    });
});
