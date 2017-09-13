require('../integration_config');

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

    utils.createChannel(channelName, null, 'single inserts');

    it('posts a single item', function (done) {
        utils.postItemQwithPayload(channelEndpoint, itemHeaders, itemContent)
            .then(function (result) {
                expect(function () {
                    var json = JSON.parse(result.body);
                    itemURL = json._links.self.href;
                }).not.toThrow();
                done();
            });
    });

    it('verifies item has correct length info', function (done) {
        expect(itemURL !== undefined).toBe(true);
        utils.getItem(itemURL, function (headers, body) {
            expect('x-item-length' in headers).toBe(true);
            var bytes = new Buffer(itemContent, 'utf-8').length;
            expect(headers['x-item-length']).toBe(bytes.toString());
            expect(body.toString()).toEqual(itemContent);
            done();
        });
    });
});
