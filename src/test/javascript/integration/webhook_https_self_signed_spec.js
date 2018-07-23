require('../integration_config');
const { createChannel, getProp, fromObjectPath } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = 'https://' + ipAddress + ':' + port + '/';
var webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource
};
let createdChannel = false;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the https endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 */
describe(testName, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, testName);
        if (getProp('status', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    var callbackItems = [];
    var postedItems = [];
    var callbackServer;

    it('runs callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpsServer(port, function (string) {
            callbackItems.push(string);
        }, done);
    });

    it('posts items', function (done) {
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

    it('closes the callback server', function (done) {
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

});
