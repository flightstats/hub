const {
    createChannel,
    fromObjectPath,
    getHubItem,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getHubUrlBase,
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelEndpoint = `${channelUrl}/${channelName}`;
const itemHeaders = {'Content-Type': 'text/plain'};
const headers = { 'Content-Type': 'application/json' };
const testContext = {
    itemSize: 0,
    itemContent: '',
    itemURL: null,
    createdChannel: false,
};

/**
 * POST a large item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'large inserts');
        if (getProp('statusCode', channel) === 201) {
            testContext.createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
        const propertiesUrl = `${getHubUrlBase()}/internal/properties`;
        const internals = await hubClientGet(propertiesUrl, headers);
        const properties = fromObjectPath(['body', 'properties'], internals) || {};
        const maxPayloadSizeMB = parseInt(properties['app.maxPayloadSizeMB'] || '43');
        testContext.itemSize = maxPayloadSizeMB - 1;
        testContext.itemContent = Array(testContext.itemSize).join('a');
    });

    it('posts a large item', async () => {
        const { createdChannel } = testContext;
        if (!createdChannel) return fail('channel not created in before block');
        const { itemContent } = testContext;
        const response = await hubClientPost(channelEndpoint, itemHeaders, itemContent);
        const body = getProp('body', response);
        const itemURL = fromObjectPath(['_links', 'self', 'href'], body);
        expect(itemURL).toBeDefined();
        testContext.itemURL = itemURL;
    });

    it('verifies item has correct length info', async () => {
        const { itemSize, itemURL } = testContext;
        if (!itemURL) {
            expect(itemURL).toBeDefined();
            return false;
        }
        const result = await getHubItem(itemURL);
        console.log('headers', getProp('headers', result));
        const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
        const bytes = itemSize - 1; // not sure why the -1 is needed. stole this from insert_and_fetch_large_spec.js
        expect(xItemLength).toBe(bytes.toString());
    }, 5 * 60 * 1000);

    afterAll(async () => {
        await hubClientDelete(`${channelUrl}/${channelName}`);
    });
});
