require('../integration_config');
const { createChannel, getProp, hubClientGet } = require('../lib/helpers');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a stream on that channel
 * 3 - post items into the channel
 * 4 - verify that the item payloads are returned within delta time
 */

xdescribe(__filename, function () {
    const callbackItems = [];
    const postedItems = [];
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, false, __filename);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('opens a stream', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const url = `${channelResource}/stream`;
        const headers = { "Content-Type": "application/json" };

        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toBe(200);
        // console.log('body', body);
    });

    it('inserts multiple items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                done();
            });
    });

    it('waits for the data', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies we got the correct number of items', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        expect(callbackItems.length).toEqual(4);
    });
});
