const {
    fromObjectPath,
    getProp,
    hubClientGet,
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
let itemURL = null;
let itemResponse = {};

describe(__filename, function () {
    beforeAll(async () => {
        // create the channel
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, headers, body);
        if (getProp('statusCode', response) === 201) {
            // insert an item in the channel
            itemResponse = await hubClientPost(channelResource, {}, messageText);
            itemURL = fromObjectPath(['body', '_links', 'self', 'href'], itemResponse);
        }
    });

    it('inserted and item', async () => {
        expect(getProp('statusCode', itemResponse)).toEqual(201);
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], itemResponse);
        expect(channelLink).toEqual(channelResource);
    });

    it('verifies the correct Content-Type header is returned', async () => {
        if (!itemURL) return fail('itemURL failed initialization in previous test');
        const response = await hubClientGet(itemURL);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/octet-stream');
        expect(getProp('body', response)).toContain(messageText);
    });
});
