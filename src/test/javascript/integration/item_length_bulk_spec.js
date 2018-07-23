require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getHubItem,
    getProp,
} = require('../lib/helpers');
/**
 * POST bulk items, GET each one, and verify the "X-Item-Length"
 * header is present with the correct values
 */

describe(__filename, function () {
    var channelName = utils.randomChannelName();
    var channelEndpoint = channelUrl + '/' + channelName + '/bulk';
    var bulkHeaders = {'Content-Type': 'multipart/mixed; boundary=oxoxoxo'};
    var itemOneContent = '{"foo":"bar"}';
    var itemTwoContent = 'foo, bar?';
    var bulkContent =
        '--oxoxoxo\r\n' +
        'Content-Type: application/json\r\n' +
        '\r\n' + itemOneContent + '\r\n' +
        '--oxoxoxo\r\n' +
        'Content-Type: text/plain\r\n' +
        '\r\n' + itemTwoContent + '\r\n' +
        '--oxoxoxo--';
    var itemURLs = [];
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'bulk inserts');
        if (getProp('status', channel) === 201) {
            console.log(`created channel for ${__filename}`);
            createdChannel = true;
        }
    });

    it('posts items in bulk', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQwithPayload(channelEndpoint, bulkHeaders, bulkContent)
            .then(function (result) {
                let json = {};
                try {
                    json = JSON.parse(getProp('body', result));
                } catch (ex) {
                    console.log('error parsing json: ', ex);
                }
                const uris = fromObjectPath(['_links', 'uris'], json);
                expect(uris).toBeDefined();
                itemURLs = fromObjectPath(['_links', 'uris'], json) || [];
                expect(itemURLs.length).toBe(2);
                done();
            });
    });

    it('verifies first item has correct length info', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        try {
            const result = await getHubItem(itemURLs[0]);
            expect(getProp('status', result)).toBe(200);
            const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
            expect(!!xItemLength).toBe(true);
            var bytes = Buffer.from(itemOneContent).length;
            expect(xItemLength).toBe(bytes.toString());
            const data = getProp('data', result) || {};
            expect(JSON.stringify(data)).toEqual(itemOneContent);
        } catch (ex) {
            expect(ex).toBeNull();
        }
    });

    it('verifies second item has correct length info', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        try {
            const result = await getHubItem(itemURLs[1]);
            const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
            expect(!!xItemLength).toBe(true);
            // TODO: new Buffer is deprecated
            var bytes = Buffer.from(itemTwoContent).length;
            expect(xItemLength).toBe(bytes.toString());
            const data = getProp('data', result) || {};
            expect(data).toEqual(itemTwoContent);
        } catch (ex) {
            expect(ex).toBeNull();
        }
    });
});
