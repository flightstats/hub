require('../integration_config');
const { createChannel, getProp, fromObjectPath } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
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
 * 3 - start a server at the endpoint
 * 4 - post item into the channel
 * 5 - delete the webhook
 * 6 - recreate the webhook
 * 7 - post item - should only see new item
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

    utils.itSleeps(500);

    it('starts the callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            callbackItems.push(string);
        }, done);
    });

    it('inserts an item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    utils.deleteWebhook(webhookName);

    utils.addItem(channelResource);

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    utils.itSleeps(500);

    it('inserts an item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies we got what we expected through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(2);
        let uriA;
        let uriB;
        try {
            const itemA = JSON.parse(callbackItems[0]);
            const itemB = JSON.parse(callbackItems[1]);
            const urisA = getProp('uris', itemA);
            const urisB = getProp('uris', itemB);
            uriA = urisA && urisA[0];
            uriB = urisB && urisB[0];
        } catch (ex) {
            expect(`failed to parse json, ${ex}`).toBeNull();
        }
        expect(uriA).toBe(postedItems[0]);
        expect(uriB).toBe(postedItems[1]);
    });

    it('closes the callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });
});
