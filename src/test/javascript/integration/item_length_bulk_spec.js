require('../integration_config');
const {
    fromObjectPath,
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

    utils.createChannel(channelName, null, 'bulk inserts');

    it('posts items in bulk', function (done) {
        utils.postItemQwithPayload(channelEndpoint, bulkHeaders, bulkContent)
            .then(function (result) {
                let json = {};
                try {
                    json = JSON.parse(result.body);
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

    it('verifies first item has correct length info', function (done) {
        utils.getItem(itemURLs[0], function (headers, body) {
            const xItemLength = getProp('x-item-length', headers);
            expect(!!xItemLength).toBe(true);
            // TODO: new Buffer is deprecated
            var bytes = new Buffer(itemOneContent, 'utf-8').length;
            expect(xItemLength).toBe(bytes.toString());
            const responseBody = body && body.toString();
            expect(responseBody).toEqual(itemOneContent);
            done();
        });
    });

    it('verifies second item has correct length info', function (done) {
        utils.getItem(itemURLs[1], function (headers, body) {
            const xItemLength = getProp('x-item-length', headers);
            expect(!!xItemLength).toBe(true);
            // TODO: new Buffer is deprecated
            var bytes = new Buffer(itemTwoContent, 'utf-8').length;
            expect(xItemLength).toBe(bytes.toString());
            const responseBody = body && body.toString();
            expect(responseBody).toEqual(itemTwoContent);
            done();
        });
    });
});
