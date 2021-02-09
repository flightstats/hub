const {
    fromObjectPath,
    getProp,
    hubClientDelete,
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
byteData = Buffer.alloc(1024, 'test ', 'binary');

describe(__filename, function () {
    beforeAll(async () => {
        const body = { 'name': channelName };
        const contentJSON = { 'Content-Type': 'application/json' };
        await hubClientPost(channelUrl, contentJSON, body);
    });

    it('handles binary data', async () => {
        const postedData = await hubClientPost(channelResource, { 'Content-Type': 'image/jpeg' }, byteData);
        expect(getProp('statusCode', postedData)).toEqual(201);
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], postedData);
        expect(channelLink).toEqual(channelResource);
        const itemURL = fromObjectPath(['body', '_links', 'self', 'href'], postedData);
        if (!itemURL) return fail('itemURL is not defined by test setup');
        const response = await hubClientGet(itemURL, {}, true);
        const responseBody = getProp('body', response) || '';
        expect(getProp('statusCode', response)).toEqual(200);
        expect(responseBody).toEqual(byteData);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
