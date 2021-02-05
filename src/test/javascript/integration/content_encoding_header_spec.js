const {
    hubClientDelete,
    hubClientGet,
    fromObjectPath,
    getProp,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const messageText = "Testing that the Content-Encoding header is returned";

describe(__filename, function () {
    it('creates a channel', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('inserts an item', async () => {
        const response = await hubClientPost(channelResource, {}, messageText);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verifies the Content-Encoding header is returned', async () => {
        const headers = { 'accept-encoding': 'gzip' };
        const response = await hubClientGet(channelResource, headers);
        console.log('response', response);
        const contentEncoding = response.header('content-encoding');
        expect(contentEncoding).toEqual('gzip');
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
