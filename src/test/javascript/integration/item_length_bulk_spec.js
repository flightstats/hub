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
 * POST bulk items, GET each one, and verify the "X-Item-Length"
 * header is present with the correct values
 */

describe(__filename, function () {
    const channelName = randomChannelName();
    const channelEndpoint = `${channelUrl}/${channelName}/bulk`;
    const bulkHeaders = { 'Content-Type': 'multipart/mixed; boundary=oxoxoxo' };
    const itemOneContent = '{"foo":"bar"}';
    const itemTwoContent = 'foo, bar?';
    const bulkContent = [
        '--oxoxoxo\r\n',
        'Content-Type: application/json\r\n',
        `\r\n${itemOneContent}\r\n`,
        '--oxoxoxo\r\n',
        'Content-Type: text/plain\r\n',
        `\r\n${itemTwoContent}\r\n`,
        '--oxoxoxo--',
    ].join('');
    let itemURLs = [];
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'bulk inserts');
        if (getProp('statusCode', channel) === 201) {
            console.log(`created channel for ${__filename}`);
            createdChannel = true;
        }
    });

    it('posts items in bulk', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientPost(channelEndpoint, bulkHeaders, bulkContent);
        const body = getProp('body', response);
        console.log('body', body);
        const uris = fromObjectPath(['_links', 'uris'], body);
        expect(uris).toBeDefined();
        itemURLs = fromObjectPath(['_links', 'uris'], body) || [];
        expect(itemURLs.length).toBe(2);
    });

    it('verifies first item has correct length info', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        try {
            const result = await getHubItem(itemURLs[0]);
            expect(getProp('statusCode', result)).toBe(200);
            const location = result.header('x-item-length');
            expect(!!xItemLength).toBe(true);
            const bytes = Buffer.from(itemOneContent).length;
            expect(xItemLength).toBe(bytes.toString());
            const data = getProp('body', result) || {};
            expect(`${data}`).toEqual(itemOneContent);
        } catch (ex) {
            expect(ex).toBeNull();
        }
    });

    it('verifies second item has correct length info', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        try {
            const result = await getHubItem(itemURLs[1]);
            const location = result.header('x-item-length');
            expect(!!xItemLength).toBe(true);
            const bytes = Buffer.from(itemTwoContent).length;
            expect(xItemLength).toBe(bytes.toString());
            const data = getProp('body', result) || {};
            expect(`${data}`).toEqual(itemTwoContent);
        } catch (ex) {
            expect(ex).toBeNull();
        }
    });

    afterAll(async () => {
        await hubClientDelete(`${channelUrl}/${channelName}`);
    });
});
