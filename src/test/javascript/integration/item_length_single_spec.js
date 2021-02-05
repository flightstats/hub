const {
    createChannel,
    fromObjectPath,
    getHubItem,
    getProp,
    hubClientDelete,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
/**
 * POST a single item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    const channelName = randomChannelName();
    const channelEndpoint = channelUrl + '/' + channelName;
    const itemHeaders = {'Content-Type': 'text/plain'};
    const itemContent = 'this string has normal letters, and unicode characters like "\u03B1"';
    let itemURL;
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'single inserts');
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('posts a single item', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPost(channelEndpoint, itemHeaders, itemContent);
        const body = getProp('body', response);
        itemURL = fromObjectPath(['_links', 'self', 'href'], body);
        expect(itemURL).toBeDefined();
    });

    it('verifies item has correct length info', async () => {
        if (!itemURL) {
            expect(itemURL).toBeDefined();
            return false;
        }
        const result = await getHubItem(itemURL);
        const location = result.header('x-item-length');
        expect(xItemLength).toBeDefined();
        var bytes = Buffer.from(itemContent).length;
        expect(xItemLength).toBe(bytes.toString());
        const data = getProp('body', result);
        expect(`${data}`).toEqual(itemContent);
    });

    afterAll(async () => {
        await hubClientDelete(`${channelUrl}/${channelName}`);
    });
});
