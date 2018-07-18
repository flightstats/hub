require('../integration_config');
const { fromObjectPath, getProp } = require('../lib/helpers');
var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';


/**
 * This should:
 *
 * 1 - create a channel
 * 2 - add items to the channel
 * 2 - create a webhook on that channel with startItem=previous
 * 3 - start a server at the endpoint
 * 4 - post item into the channel
 * 5 - verify that the item are returned within delta time, incuding the second item posted in 2.
 */
describe(testName, function () {
    utils.createChannel(channelName, false, testName);

    utils.itSleeps(1000);
    var postedItems = [];
    var firstItem;

    function addPostedItem(value) {
        postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
        console.log('postedItems', postedItems);
    }

    it('posts initial items ' + channelResource, function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                firstItem = fromObjectPath(['body', '_links', 'self', 'href'], value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                done();
            });
    });

    utils.itSleeps(6000);

    utils.putWebhook(webhookName, {
        callbackUrl: callbackUrl,
        channelUrl: channelResource,
        startItem: 'previous'
    }, 201, testName);

    var callbackServer;
    var callbackItems = [];

    it('starts a callback server', function (done) {
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            callbackItems.push(string);
        }, done);
    });

    it('inserts items', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                done();
            });
    });

    it('waits for data', function (done) {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('closes the first callback server', function (done) {
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    it('verifies we got what we expected through the callback', function () {
        expect(callbackItems.length).toBe(5);
        expect(postedItems.length).toBe(5);
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
