require('./integration_config.js');

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
    var port = utils.getPort();

    var badConfig = {
        callbackUrl: 'http://nothing:8080/nothing',
        channelUrl: channelResource
    };
    var goodConfig = {
        callbackUrl: callbackDomain + ':' + port + '/',
        channelUrl: channelResource
    };

    utils.createChannel(channelName, false, testName);

    utils.putWebhook(webhookName, badConfig, 201, testName);

    utils.itSleeps(2000);

    var firstItemURL;
    
    it('posts the first item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                firstItemURL = value.body._links.self.href;
                done();
            });
    });

    utils.putWebhook(webhookName, goodConfig, 200, testName);

    utils.itSleeps(10000);

    var receivedItems = [];

    it('starts a callback server', function (done) {
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            receivedItems.push(string);
        }, done);
    });

    var secondItemURL;

    it('posts the second item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                secondItemURL = value.body._links.self.href;
                done();
            });
    });

    it('waits for data', function (done) {
        var sentItems = [
            firstItemURL,
            secondItemURL
        ];
        utils.waitForData(receivedItems, sentItems, done);
    });

    it('verifies we got both items through the callback', function () {
        expect(receivedItems.length).toBe(2);
        expect(receivedItems).toContain(firstItemURL);
        expect(receivedItems).toContain(secondItemURL);
    });

    it('closes the callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

});

