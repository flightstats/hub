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
const messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

describe(__filename, function () {
    beforeAll(async () => {
        // create the channel
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        await hubClientPost(channelUrl, headers, body);
    });

    it('inserts an item', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const response = await hubClientPost(channelResource, headers, messageText);
        expect(getProp('statusCode', response)).toEqual(201);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], response);
        const timestamp = fromObjectPath(['body', 'timestamp'], response);
        expect(contentType).toEqual('application/json');
        expect(channelLink).toEqual(channelResource);
        expect(timestamp).toMatch(/^\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\d\.\d\d\dZ$/);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
