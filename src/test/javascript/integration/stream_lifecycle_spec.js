const {
    createChannel,
    getProp,
    hubClientGet,
    hubClientPostTestItem,
    waitForCondition,
    randomChannelName,
} = require('../lib/helpers');
const channelName = randomChannelName();
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
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
    });

    it('inserts multiple items', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const value = () => hubClientPostTestItem(channelResource);
        const values = [1, 2, 3, 4].map(v => value());
        const responses = await Promise.all(values);
        postedItems.push(responses);
        const condition = () => (callbackItems.length === postedItems.length);
        await waitForCondition(condition);
    });

    it('verifies we got the correct number of items', function () {
        if (!createdChannel) return fail('channel not created in before block');
        expect(callbackItems.length).toEqual(4);
    });
});
