require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

describe(__filename, function () {
    it('creates the channel without a name', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': '' };
        const response = await hubClientPost(getChannelUrl(), headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
