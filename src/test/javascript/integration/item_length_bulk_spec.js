require('../integration_config');

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
                expect(function () {
                    var json = JSON.parse(result.body);
                    itemURLs = json._links.uris;
                }).not.toThrow();
                expect(itemURLs.length).toBe(2);
                done();
            });
    });

    it('verifies first item has correct length info', function (done) {
        utils.getItem(itemURLs[0], function (headers, body) {
            expect('x-item-length' in headers).toBe(true);
            var bytes = new Buffer(itemOneContent, 'utf-8').length;
            expect(headers['x-item-length']).toBe(bytes.toString());
            expect(body.toString()).toEqual(itemOneContent);
            done();
        });
    });

    it('verifies second item has correct length info', function (done) {
        utils.getItem(itemURLs[1], function (headers, body) {
            expect('x-item-length' in headers).toBe(true);
            var bytes = new Buffer(itemTwoContent, 'utf-8').length;
            expect(headers['x-item-length']).toBe(bytes.toString());
            expect(body.toString()).toEqual(itemTwoContent);
            done();
        });
    });
});
