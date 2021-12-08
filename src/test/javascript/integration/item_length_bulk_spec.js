const crypto = require('crypto')
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
/**
 * POST bulk items, GET each one, and verify the "X-Item-Length"
 * header is present with the correct values
 */
const channelUrl = getChannelUrl();
const channelName = randomChannelName();
let originalTimeout;

describe(__filename, function () {
    beforeEach(function() {
        originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
        jasmine.DEFAULT_TIMEOUT_INTERVAL = 100000;
    });
    it('posts items in bulk', async () => {
        // given
        const channelEndpoint = `${channelUrl}/${channelName}/bulk`;
        const bulkHeaders = { 'Content-Type': 'multipart/mixed; boundary=oxoxoxo' };
        const boundary = '--oxoxoxo';
        const contentType = 'Content-Type: application/octet-stream';
        const items = [...Array(80).keys()].map(() => crypto.randomBytes(500 * 1024).toString('base64'));
        const bulkContent = [
            ...items.map((item, index) => `${boundary}\r\n${contentType}\r\n\r\n${item}\r\n`),
            `${boundary}--`
        ].join('')
        // create channel
        const channel = await createChannel(channelName, null, 'bulk inserts');
        if (getProp('statusCode', channel) === 201) {
            console.log(`created channel for ${__filename}`);
        } else {
            return fail('channel not created in before block');
        }
        try {
            // when
            // post item
            const postResponse = await hubClientPost(channelEndpoint, bulkHeaders, bulkContent);
            const postBody = getProp('body', postResponse);
            console.log('body', postBody);
            const itemURLs = fromObjectPath(['_links', 'uris'], postBody) || [];

            // then
            itemURLs.forEach(async (item, index) => {
                const result = await getHubItem(item);
                expect(getProp('statusCode', result)).toBe(200);
                const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
                expect(xItemLength).toBeTruthy();
                const bytes = Buffer.from(items[index]).length;
                expect(xItemLength).toBe(bytes.toString());
                const data = getProp('body', result) || {};
                expect(`${data}`).toEqual(items[index]);
                console.log('data', data.toString());
            });
        } catch (ex) {
            return fail(`test failed with error: ${ex}`);
        }
    });
    afterEach(async () => {
        jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
        await hubClientDelete(`${channelUrl}/${channelName}`);
    });
});
