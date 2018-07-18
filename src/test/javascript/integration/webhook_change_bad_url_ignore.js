require('../integration_config');
const { getProp, fromObjectPath } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

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
        channelUrl: channelResource
    };
    var goodConfig = {
        callbackUrl: callbackDomain + ':' + port + '/',
        channelUrl: channelResource
    };

    utils.createChannel(channelName, false, testName);

    utils.putWebhook(webhookName, badConfig, 201, testName);

    utils.itSleeps(2000);

    utils.putWebhook(webhookName, goodConfig, 200, testName);

    utils.itSleeps(10000);

    var receivedItems = [];

    it('starts a callback server', function (done) {
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            receivedItems.push(string);
        }, done);
    });

    var itemURL;

    it('posts and item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                itemURL = fromObjectPath(['body', '_links', 'self', 'href'], value);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(receivedItems, [itemURL], done);
    });

    it('verifies we got the item through the callback', function () {
        expect(receivedItems.length).toBe(1);
        const receivedItem = receivedItems.find(item => item.includes(item));
        expect(receivedItem).toBeDefined();
        expect(receivedItem).toContain(itemURL);
    });

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

});
