require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');
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
        let body = {"mutableTime": moment().subtract(1, 'minute').toISOString()};
        utils.httpPut(channelResource, headers, body)
            .then(response => expect(getProp('statusCode', response)).toEqual(201))
            .finally(done);
    });

    it('posts a historical item', function (done) {
        utils.postItemQwithPayload(historicalEndpoint, itemHeaders, itemContent)
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
            const xItemLength = getProp('x-item-length', headers);
            expect(!!xItemLength).toBe(true);
            // TODO: new Buffer is deprecated
            var bytes = new Buffer(itemContent, 'utf-8').length;
            expect(xItemLength).toBe(bytes.toString());
            const responseBody = body && body.toString();
            expect(responseBody).toEqual(itemContent);
            done();
        });
    });
});
