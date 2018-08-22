require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
    hubClientPost,
    hubClientPut,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const headers = { 'Content-Type': 'application/json' };
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
/**
 * create a channel via put with allowZeroBytes === true
 * Post a zero byte item.
 * change the property to false.
 * verify that we can not post a zero byte item.
 *
 */
describe(__filename, function () {
    it('creates a channel with method PUT', async () => {
        const response = await hubClientPut(channelResource, headers, {});
        const body = getProp('body', response);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], body);
        const allowZeroBytes = getProp('allowZeroBytes', body);
        expect(selfLink).toEqual(channelResource);
        expect(allowZeroBytes).toEqual(true);
    });

    it("adds zero byte item", async () => {
        const response = await hubClientPost(channelResource, { "Content-Type": "text/plain" });
        expect(getProp('statusCode', response)).toBe(201);
    });

    it('updates the channel to allowZeroBytes: false', async () => {
        const response = await hubClientPut(channelResource, headers, { allowZeroBytes: false });
        const body = getProp('body', response);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], body);
        const allowZeroBytes = getProp('allowZeroBytes', body);
        expect(selfLink).toEqual(channelResource);
        expect(allowZeroBytes).toEqual(false);
    });

    it('waits while the channel is refreshed', async () => {
        const response = await hubClientChannelRefresh();
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it("fails to add zero byte item", async () => {
        const response = await hubClientPost(channelResource, { "Content-Type": "text/plain" });
        expect(getProp('statusCode', response)).toBe(400);
    });
});
