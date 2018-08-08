require('../integration_config');
const { createChannel, getProp, fromObjectPath } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;
let createdChannel = false;
/**
 * This is disabled for now.
 *
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel with a non-existent endpointA
 * 3 - re-create the webhook with the same name and a new endpointB
 * 4 - post item into the channel
 * 5 - start a server at the endpointB
 * 6 - post item - should see item at endPointB
 */
describe(testName, function () {
    var callbackServer;
    var port = utils.getPort();
    var badConfig = {
        callbackUrl: 'http://localhost:8080/nothing',
        channelUrl: channelResource,
    };
    var goodConfig = {
        callbackUrl: callbackDomain + ':' + port + '/',
        channelUrl: channelResource,
    };
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, testName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    utils.putWebhook(webhookName, badConfig, 201, testName);

    utils.itSleeps(2000);

    utils.putWebhook(webhookName, goodConfig, 200, testName);

    utils.itSleeps(10000);

    var receivedItems = [];

    it('starts a callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            receivedItems.push(string);
        }, done);
    });

    var itemURL;

    it('posts and item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                itemURL = fromObjectPath(['body', '_links', 'self', 'href'], value);
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(receivedItems, [itemURL], done);
    });

    it('verifies we got the item through the callback', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(receivedItems.length).toBe(1);
        const receivedItem = receivedItems.find(item => item.includes(item));
        expect(receivedItem).toBeDefined();
        expect(receivedItem).toContain(itemURL);
    });

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.closeServer(callbackServer, done);
    });

});
