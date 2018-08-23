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
const messageText = `"MY SUPER TEST CASE: this & <that>. " ${Math.random()}`;
let itemURL = null;
let itemResponse = {};

describe(__filename, function () {
    beforeAll(async () => {
        const contentJSON = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, contentJSON, body);
        if (getProp('statusCode', response) === 201) {
            const headers = { 'Content-Type': 'application/fractals' };
            itemResponse = await hubClientPost(channelResource, headers, messageText);
            itemURL = fromObjectPath(['body', '_links', 'self', 'href'], itemResponse);
        }
    });

    it('inserted an item', async () => {
        expect(getProp('statusCode', itemResponse)).toEqual(201);
        const contentType = fromObjectPath(['headers', 'content-type'], itemResponse);
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], itemResponse);
        expect(contentType).toEqual('application/json');
        expect(channelLink).toEqual(channelResource);
    });

    it('verifies an error is returned when content-type doesn\'t match the accept header', async () => {
        if (!itemURL) return fail('itemURL failed initialization in previous test');
        const headers = { 'Accept': 'application/json' };
        const response = await hubClientGet(itemURL, headers);
        expect(getProp('statusCode', response)).toEqual(406);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
