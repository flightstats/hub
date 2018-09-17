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
const messageText = `MY SUPER TEST CASE: this & <that>. ${Math.random()}`;
let itemURL = null;
let itemResponse = {};
describe(__filename, function () {
    beforeAll(async () => {
        const contentJSON = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, contentJSON, body);
        if (getProp('statusCode', response) === 201) {
            const contentFractals = { 'Content-Type': 'application/fractals' };
            itemResponse = await hubClientPost(channelResource, contentFractals, messageText);
            itemURL = fromObjectPath(['body', '_links', 'self', 'href'], itemResponse);
        }
    });

    it('inserted an item', async () => {
        expect(getProp('statusCode', itemResponse)).toEqual(201);
        const contentType = fromObjectPath(['headers', 'content-type'], itemResponse);
        expect(contentType).toEqual('application/json');
        const channelLink = fromObjectPath(['body', '_links', 'channel', 'href'], itemResponse);
        expect(channelLink).toEqual(channelResource);
    });

    it('verifies the correct content-type is returned', async () => {
        if (!itemURL) return fail('itemURL failed initialization in previous test');
        const response = await hubClientGet(itemURL);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        expect(contentType).toEqual('application/fractals');
        expect(getProp('body', response)).toContain(messageText);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
