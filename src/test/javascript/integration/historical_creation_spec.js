const moment = require('moment');
const {
    getProp,
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
const tag = Math.random().toString().replace(".", "");
const headers = { 'Content-Type': 'application/json' };
const channelResource = `${channelUrl}/${channel}`;
/**
 * This should:
 * Create a channel with mutableTime
 * verify that mutableTime is returned from Put
 * verify that mutableTime is returned from Get
 *
 */
describe(__filename, function () {
    const mutableTime = moment.utc().subtract(1, 'hours').format('YYYY-MM-DDTHH:mm:ss');
    const expected = `${mutableTime}.000Z`;
    const channelBody = {
        mutableTime: mutableTime,
        tags: [tag, "test"],
    };
    it('creates a channel with mutableTime', async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const body = getProp('body', response);
        expect(getProp('ttlDays', body)).toBe(0);
        expect(getProp('maxItems', body)).toBe(0);
        expect(getProp('mutableTime', body)).toBe(expected);
    });

    it('verify mutabelTime returned from GET method on channel', async () => {
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
