require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientPut,
} = require('../lib/helpers');
const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const port = utils.getPort();
const callbackUrl = `${callbackDomain}:${port}/`;
let createdChannel = false;
/**
 * This should:
 *
 * 1 - create a channel
 * 2 - add items to the channel
 * 2 - create a webhook on that channel
 * 3 - start a server at the endpoint
 * 4 - post items into the channel
 * 5 - verify that the item are returned within delta time, excluding items posted in 2.
 */
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    utils.itSleeps(1000);
    const postedItems = [];
    let firstItem = null;

    function addPostedItem (value) {
        postedItems.push(fromObjectPath(['body', '_links', 'self', 'href'], value));
        console.log('postedItems', postedItems);
    }

    it('posts initial items ' + channelResource, function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
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

    it('creates a webhook', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const url = `${utils.getWebhookUrl()}/${webhookName}`;
        const headers = { 'Content-Type': 'application/json' };
        const body = {
            callbackUrl,
            channelUrl: channelResource,
            startItem: firstItem,
        };

        const response = await hubClientPut(url, headers, body);
        const responseBody = getProp('body', response) || {};
        const location = fromObjectPath(['headers', 'location'], response);
        expect(getProp('statusCode', response)).toBe(201);
        expect(location).toBe(url);
        expect(responseBody.callbackUrl).toBe(callbackUrl);
        expect(responseBody.channelUrl).toBe(channelResource);
        expect(responseBody.name).toBe(webhookName);
    });

    var callbackServer;
    var callbackItems = [];

    it('starts a callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, function (string) {
            console.log('called webhook ' + webhookName + ' ' + string);
            callbackItems.push(string);
        }, done);
    });

    it('inserts items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
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
