require('../integration_config');

var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel with a non-existent endpointA
 * 3 - post item into the channel
 * 4 - change the webhook with the same name and a new endpointB
 * 5 - start a server at the endpointB
 * 6 - post item - should see items at endPointB
 */

describe(testName, function () {

    var callbackServer;
    var portB = utils.getPort();

    var itemsB = [];
    var postedItem;
    var badConfig = {
        callbackUrl: 'http://nothing:8080/nothing',
        channelUrl: channelResource
    };
    var goodConfig = {
        callbackUrl: callbackDomain + ':' + portB + '/',
        channelUrl: channelResource
    };

    utils.createChannel(channelName, false, testName);

    utils.putWebhook(webhookName, badConfig, 201, testName);

    utils.itSleeps(2000);

    utils.addItem(channelResource);

    utils.putWebhook(webhookName, goodConfig, 200, testName);

    utils.itSleeps(10000);

    it('starts a callback server', function (done) {
        callbackServer = utils.startHttpServer(portB, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            itemsB.push(string);
        }, done);
    });

    it('inserts an item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItem = value.body._links.self.href;
                done();
            });
    });

    it('waits for data', function (done) {
        var sentItems = [
            'we dont care about the first item',
            postedItem
        ];
        utils.waitForData(itemsB, sentItems, done);
    });

    it('verifies we got both items through the callback', function () {
        expect(itemsB.length).toBe(2);
        expect(JSON.parse(itemsB[1]).uris[0]).toBe(postedItem);
    });

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

});

