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
    itemURL: null,
};

/**
 * POST a large item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

const getMaxPayLoadMB = async () => {
    const propertiesUrl = `${getHubUrlBase()}/internal/properties`;
    const internals = await hubClientGet(propertiesUrl, headers);
    const properties = fromObjectPath(['body', 'properties'], internals) || {};
    return parseInt(properties['app.maxPayloadSizeMB'] || '43');
}

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'large inserts');
        let createdChannel = false;
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
        const maxPayloadSizeMB = await getMaxPayLoadMB();
        testContext.itemSize = maxPayloadSizeMB - 1;
        const itemContent = Array(testContext.itemSize).join('a');

        // post a large item
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPost(channelEndpoint, itemHeaders, itemContent);
        testContext.itemURL = fromObjectPath(['body', '_links', 'self', 'href'], response);
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
