require('../integration_config');

const moment = require('moment');

/**
 * POST a historical item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    var oneDayAgo = moment().subtract(1, 'days');
    var pathPattern = 'YYYY/MM/DD/HH/mm/ss/SSS';
    var channelName = utils.randomChannelName();
    var channelResource = `${channelUrl}/${channelName}`;
    var historicalEndpoint = `${channelResource}/${oneDayAgo.format(pathPattern)}`;
    var itemHeaders = {'Content-Type': 'text/plain'};
    var itemContent = 'this is a string for checking length on historical inserts';
    var itemURL;

    it('creates a channel', (done) => {
        let headers = {'Content-Type': 'application/json'};
        let body = {"mutableTime": moment().toISOString()};
        utils.httpPut(channelResource, headers, body)
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('posts a historical item', function (done) {
        utils.postItemQwithPayload(historicalEndpoint, itemHeaders, itemContent)
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
