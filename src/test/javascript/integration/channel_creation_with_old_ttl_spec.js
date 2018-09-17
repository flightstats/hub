const {
    getProp,
    hubClientDelete,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');
const name = randomChannelName();

describe(__filename, function () {
    it('creates a channel with an old TTL', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = {
            name,
            ttlMillis: 0,
        };
        const response = await hubClientPost(getChannelUrl(), headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    afterAll(async () => {
        await hubClientDelete(`${getChannelUrl()}/${name}`);
    });
});
