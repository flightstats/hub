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
let channelName;
let originalTimeout;
let channelEndpoint;
const bulkHeaders = { 'Content-Type': 'multipart/mixed; boundary=oxoxoxo' };
const boundary = '--oxoxoxo';
const contentType = 'Content-Type: application/octet-stream';

describe(__filename, function () {
    beforeEach(function() {
        channelName = randomChannelName();
        channelEndpoint = `${channelUrl}/${channelName}/bulk`;
        originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
        jasmine.DEFAULT_TIMEOUT_INTERVAL = 1000000;
    });

    it('fails if the item is bigger than configured max upload size', async () => {
        // as configured the hub accepts payloads smaller than 120MB
        // given
        const items = [...Array(120).keys()].map(() => crypto.randomBytes(1024 * 1024).toString('binary'));
        const bulkContent = [
            ...items.map((item, index) => `${boundary}\r\n${contentType}\r\n\r\n${item}\r\n`),
            `${boundary}--`
        ].join('')
        // create channel
        const channel = await createChannel(channelName, null, 'bulk inserts');
        if (getProp('statusCode', channel) != 201) {
            return fail('channel not created in before block');
        }
        console.log(`created channel ${channelName} for ${__filename}`);

        // when
        // post item
        console.log("item size is ", `${parseFloat((bulkContent.length / 1024 / 1024).toFixed(2), 10)}MB`)
        const postResponse = await hubClientPost(channelEndpoint, bulkHeaders, bulkContent);
        const postBody = getProp('body', postResponse);
        console.log('body', postBody);
        expect(postBody).toEqual("max payload size is 125829120 bytes")
        expect(getProp('statusCode', postResponse)).toEqual(413)
    });



    it('posts items in bulk', async () => {
        // given
        const items = [...Array(90).keys()].map(() => crypto.randomBytes(1023 * 1023).toString('base64'));
        const bulkContent = [
            ...items.map((item, index) => `${boundary}\r\n${contentType}\r\n\r\n${item}\r\n`),
            `${boundary}--`
        ].join('')
        // create channel
        const channel = await createChannel(channelName, null, 'bulk inserts');
        if (getProp('statusCode', channel) != 201) {
            return fail('channel not created in before block');
        }
        console.log(`created channel ${channelName} for ${__filename}`);
        try {
            // when
            // post item
            console.log("item size is ", `${parseFloat((bulkContent.length / 1024 / 1024).toFixed(2), 10)}MB`)
            const postResponse = await hubClientPost(channelEndpoint, bulkHeaders, bulkContent);
            const postBody = getProp('body', postResponse);
            console.log('body', postBody);
            if (getProp('statusCode', postResponse) != 201) {
                return fail('item not posted correctly');
            }
            const itemURLs = fromObjectPath(['_links', 'uris'], postBody) || [];
            // then
            itemURLs.forEach(async (item, index) => {
                const result = await getHubItem(item);
                expect(getProp('statusCode', result)).toBe(200);
                const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
                expect(xItemLength).toBeDefined();
                const bytes = Buffer.from(items[index]).length;
                expect(xItemLength).toEqual(bytes.toString());
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
