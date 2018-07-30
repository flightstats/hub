require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');

describe(__filename, function () {
    it('creates a channel with no name', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = {};
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
