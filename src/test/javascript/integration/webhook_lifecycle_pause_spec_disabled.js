// bc -- this test does not run reliably due to what appears to be timing issues.
const { createChannel, fromObjectPath, getProp } = require('../lib/helpers');
require('../integration_config');

var channelName = utils.randomChannelName();
var webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
var testName = __filename;
var port = utils.getPort();
var callbackUrl = callbackDomain + ':' + port + '/';
var webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: false,
};

var webhookConfigPaused = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: true,
};
let createdChannel = false;
/**
 *
 * This should:
 *
 * 1 - create a channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the records are returned within delta time
 * 6 - pause the webhook
 * 7 - post items into the channel
 * 8 - verify that no records are returned within delta time
 * 9 - un-pause the webhook
 * 10 - verify that the records are returned within delta time
 */
describe(testName, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, testName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });
    var callbackItems = [];
    var postedItems = [];

    function addPostedItem (value) {
        const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], value);
        postedItems.push(selfLink);
        console.log('value.body._links.self.href', selfLink);
    }

    utils.putWebhook(webhookName, webhookConfig, 201, testName);

    var callbackServer;

    it('starts a callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            callbackItems.push(string);
            console.log(callbackItems.length, 'called back', string);
        }, done);
    });

    it('posts two items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            }).then(function (value) {
                addPostedItem(value);
                done();
            });
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    }, 15 * 1000);

    utils.itSleeps(2000);

    it('expects 2 items collected', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(2);
    });

    console.log("###### pausing web hook");
    utils.putWebhook(webhookName, webhookConfigPaused, 200, testName);

    utils.itSleeps(2000);

    it('posts items to paused ' + webhookName, function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                addPostedItem(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                addPostedItem(value);
                done();
            });
    }, 3000);

    utils.itSleeps(500);

    // we added another 2 to a paused web hook.  should still be 2
    it('verfies number ' + webhookName, function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(2);
    });

    console.log("###### resuming web hook");
    utils.putWebhook(webhookName, webhookConfig, 200, testName);

    utils.itSleeps(2000);

    it('closes the callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackServer).toBeDefined();
        utils.closeServer(callbackServer, done);
    });

    it('verifies posted items were received', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(4);
        expect(postedItems.length).toBe(4);
        // for (var i = 0; i < callbackItems.length; i++) {
        //     var parse = JSON.parse(callbackItems[i]);
        //     expect(parse.uris[0]).toBe(postedItems[i]);
        //     expect(parse.name).toBe(webhookName);
        // }
    });

    utils.deleteWebhook(webhookName);
});
