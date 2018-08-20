// bc -- this test does not run reliably due to what appears to be timing issues.
const {
    createChannel,
    deleteWebhook,
    fromObjectPath,
    getProp,
    hubClientPostTestItem,
    itSleeps,
    putWebhook,
} = require('../lib/helpers');
require('../integration_config');

const channelName = utils.randomChannelName();
const webhookName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const port = utils.getPort();
const callbackUrl = `${callbackDomain}:${port}/`;
const webhookConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: false,
};

const webhookConfigPaused = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource,
    paused: true,
};
let createdChannel = false;
let callbackServer = null;
const callbackItems = [];
const postedItems = [];
const addPostedItem = (value) => {
    const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], value);
    postedItems.push(selfLink);
    console.log('value.body._links.self.href', selfLink);
};
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
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('creates the webhook', async () => {
        const response = await putWebhook(webhookName, webhookConfig, 201, __filename);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('starts a callback server', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        callbackServer = utils.startHttpServer(port, (string) => {
            callbackItems.push(string);
            console.log(callbackItems.length, 'called back', string);
        }, done);
    });

    it('posts two items', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response0 = await hubClientPostTestItem(channelResource);
        addPostedItem(response0);
        const response1 = await hubClientPostTestItem(channelResource);
        addPostedItem(response1);
    });

    it('waits for data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    }, 15 * 1000);

    it('waits 2000 ms', async () => {
        await itSleeps(2000);
    });

    it('expects 2 items collected', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(2);
    });

    it('pauses the webhook', async () => {
        console.log('###### pausing web hook');
        const response = await putWebhook(webhookName, webhookConfigPaused, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('waits 2000 ms', async () => {
        await itSleeps(2000);
    });

    it(`posts items to paused ${webhookName}`, async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response0 = await hubClientPostTestItem(channelResource);
        addPostedItem(response0);
        const response1 = await hubClientPostTestItem(channelResource);
        addPostedItem(response1);
    }, 3000);

    it('waits 500 ms', async () => {
        await itSleeps(500);
    });

    // we added another 2 to a paused web hook.  should still be 2
    it(`verfies number ${webhookName}`, function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toBe(2);
    });

    it('unpauses the webhook', async () => {
        console.log('###### resuming web hook');
        const response = await putWebhook(webhookName, webhookConfig, 200, __filename);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    // I upped this to 5s on 08/13/2018 so that it reliably passes in local envs
    it('waits 5000 ms', async () => {
        await itSleeps(5000);
    });

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

    it('deletes the webhook', async () => {
        const response = await deleteWebhook(webhookName);
        expect(getProp('statusCode', response)).toBe(202);
    });
});
