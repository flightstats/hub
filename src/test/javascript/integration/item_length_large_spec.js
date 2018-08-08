require('../integration_config');
const {
    createChannel,
    fromObjectPath,
    getHubItem,
    getProp,
} = require('../lib/helpers');
/**
 * POST a large item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    var channelName = utils.randomChannelName();
    var channelEndpoint = channelUrl + '/' + channelName;
    var itemHeaders = {'Content-Type': 'text/plain'};
    var itemSize = 41 * 1024 * 1024;
    var itemContent = Array(itemSize).join('a');
    var itemURL;
    let createdChannel = false;

    beforeAll(async () => {
        const channel = await createChannel(channelName, null, 'large inserts');
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('posts a large item', function (done) {
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
        console.log('headers', getProp('headers', result));
        const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
        const bytes = itemSize - 1; // not sure why the -1 is needed. stole this from insert_and_fetch_large_spec.js
        expect(xItemLength).toBe(bytes.toString());
    }, 5 * 60 * 1000);

});
