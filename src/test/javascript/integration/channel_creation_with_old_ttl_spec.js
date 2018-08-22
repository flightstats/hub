require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

describe(__filename, function () {
    it('creates a channel with an old TTL', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = {
            'name': utils.randomChannelName(),
            'ttlMillis': 0,
        };
        const response = await hubClientPost(getChannelUrl(), headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });
});
