require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getHubItem,
    getProp,
} = require('../lib/helpers');
/**
 * POST a single item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    var channelName = utils.randomChannelName();
    var channelEndpoint = channelUrl + '/' + channelName;
    var itemHeaders = {'Content-Type': 'text/plain'};
    var itemContent = 'this string has normal letters, and unicode characters like "\u03B1"';
    var itemURL;
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'single inserts');
        if (getProp('status', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('posts a single item', function (done) {
        if (!createdChannel) return done.fail('channel not created in before block');
        utils.postItemQwithPayload(channelEndpoint, itemHeaders, itemContent)
            .then(function (result) {
                try {
                    const json = JSON.parse(getProp('body', result));
                    itemURL = fromObjectPath(['_links', 'self', 'href'], json);
                    expect(itemURL).toBeDefined();
                } catch (ex) {
                    expect(ex).toBeNull();
                    console.log('error parsing json: ', ex);
                }
                done();
            });
    });

    it('verifies item has correct length info', async () => {
        if (!itemURL) {
            expect(itemURL).toBeDefined();
            return false;
        }
        const result = await getHubItem(itemURL);
        const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
        expect(xItemLength).toBeDefined();
        var bytes = Buffer.from(itemContent).length;
        expect(xItemLength).toBe(bytes.toString());
        const data = getProp('data', result);
        expect(data).toEqual(itemContent);
    });
});
