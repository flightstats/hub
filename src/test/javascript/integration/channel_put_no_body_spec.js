const { getProp, hubClientDelete, hubClientPut, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('creates a channel with no information', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const response = await hubClientPut(channelResource, headers, {});
        expect(getProp('statusCode', response)).toEqual(201);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
