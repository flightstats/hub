const { getProp, hubClientPost } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
describe(__filename, function () {
    it('creates a channel with an invalid name', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': 'not valid!' };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
