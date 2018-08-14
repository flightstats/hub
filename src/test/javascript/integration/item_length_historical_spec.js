require('../integration_config');
const {
    fromObjectPath,
    getHubItem,
    getProp,
    hubClientPut,
} = require('../lib/helpers');
const moment = require('moment');

/**
 * POST a historical item, GET it, and verify the "X-Item-Length"
 * header is present with the correct value
 */

describe(__filename, function () {
    const oneDayAgo = moment().subtract(1, 'days');
    const pathPattern = 'YYYY/MM/DD/HH/mm/ss/SSS';
    const channelName = utils.randomChannelName();
    const channelResource = `${channelUrl}/${channelName}`;
    const historicalEndpoint = `${channelResource}/${oneDayAgo.format(pathPattern)}`;
    const itemHeaders = { 'Content-Type': 'text/plain' };
    const itemContent = 'this is a string for checking length on historical inserts';
    let itemURL;

    it('creates a channel', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { mutableTime: moment().subtract(1, 'minute').toISOString() };
        const response = await hubClientPut(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
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

    it('verifies item has correct length info', async () => {
        if (!itemURL) {
            expect(itemURL).toBeDefined();
            return false;
        }
        const result = await getHubItem(itemURL);
        const xItemLength = fromObjectPath(['headers', 'x-item-length'], result);
        expect(xItemLength).toBeDefined();
        const bytes = (Buffer.from(itemContent) || '').length;
        expect(xItemLength).toBe(bytes.toString());
        const data = getProp('body', result);
        expect(`${data}`).toEqual(itemContent);
    });
});
