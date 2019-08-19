const moment = require('moment');
const {
    getProp,
    hubClientChannelRefresh,
    hubClientDelete,
    hubClientGet,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = randomChannelName();
const tag = Math.random().toString().replace('.', '');
const headers = { 'Content-Type': 'application/json' };
/**
 * This should:
 * Create a channel without mutableTime
 * Change that channel to have a mutableTime
 *
 */
const channelResource = `${channelUrl}/${channel}`;
describe(__filename, () => {
    it('creates a channel (no mutableTime)', async () => {
        const response = await hubClientPut(channelResource, headers, { ttlDays: 20 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    const mutableTime = moment.utc().subtract(1, 'hours').format('YYYY-MM-DDTHH:mm:ss');
    const expected = `${mutableTime}.000Z`;

    const channelBody = {
        ttlDays: 0,
        mutableTime,
        tags: [tag, 'test'],
        storage: 'SINGLE',
    };

    it('updates the channel to have a mutableTime', async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        const body = getProp('body', response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(getProp('ttlDays', body)).toBe(0);
        expect(getProp('maxItems', body)).toBe(0);
        expect(getProp('mutableTime', body)).toBe(expected);
    });

    it('waits while the channel is refreshed', async () => {
        const response = await hubClientChannelRefresh();
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('verifies the mutableTime time change after channel refresh', async () => {
        const response = await hubClientGet(channelResource, headers);
        const body = getProp('body', response);
        expect(getProp('ttlDays', body)).toBe(0);
        expect(getProp('maxItems', body)).toBe(0);
        expect(getProp('mutableTime', body)).toBe(expected);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
