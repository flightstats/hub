const {
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPut,
    hubClientPostTestItem,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };

/**
 * create a channel
 * post two items
 * stream both items back with batch
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts items to the channel', async () => {
        const response1 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response1)).toEqual(201);

        const response2 = await hubClientPostTestItem(channelResource);
        expect(getProp('statusCode', response2)).toEqual(201);
    });

    it('gets zip items ', async () => {
        const response = await hubClientGet(`${channelResource}/latest/10?stable=false&batch=true`, { Accept: 'application/zip' });
        expect(getProp('statusCode', response)).toBe(200);
        // todo - gfm - 8/19/15 - parse zip
        expect(getProp('body', response)).toBeDefined();
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
