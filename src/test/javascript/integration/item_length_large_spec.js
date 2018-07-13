require('../integration_config');
const {
    fromObjectPath,
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

    utils.createChannel(channelName, null, 'large inserts');

    it('posts a large item', function (done) {
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

    it('verifies item has correct length info', function (done) {
        expect(itemURL !== undefined).toBe(true);
        utils.getItem(itemURL, function (headers, body) {
            console.log('headers:', headers);
            const xItemLength = getProp('x-item-length', headers);
            expect(!!xItemLength).toBe(true);
            var bytes = itemSize - 1; // not sure why the -1 is needed. stole this from insert_and_fetch_large_spec.js
            expect(xItemLength).toBe(bytes.toString());
            // expect(body.toString()).toEqual(itemContent);
            done();
        });
    }, 5 * 60 * 1000);

});
