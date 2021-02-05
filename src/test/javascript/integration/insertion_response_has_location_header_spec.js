const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const messageText = `MY SUPER TEST CASE: this & <that>. ${Math.random()}`;

describe(__filename, function () {
    it('creates a channel', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('inserts an item', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const response = await hubClientPost(channelResource, headers, messageText);
        expect(getProp('statusCode', response)).toEqual(201);
        const location = response.header('location');;
        expect(location).toBeDefined();
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
