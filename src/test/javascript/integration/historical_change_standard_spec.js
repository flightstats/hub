require('../integration_config');
const { getProp, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const moment = require('moment');
const tag = Math.random().toString().replace(".", "");
const headers = { 'Content-Type': 'application/json' };
/**
 * This should:
 * Create a channel without mutableTime
 * Change that channel to have a mutableTime
 *
 */
const url = `${channelUrl}/${channel}`;
describe(__filename, function () {
    it('creates a channel (no mutableTime)', async () => {
        const response = await hubClientPut(url, headers, { ttlDays: 20 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    const mutableTime = moment.utc().subtract(1, 'hours').format('YYYY-MM-DDTHH:mm:ss');
    const expected = `${mutableTime}.000Z`;

    const channelBody = {
        ttlDays: 0,
        mutableTime: mutableTime,
        tags: [tag, "test"],
    };

    it('updates the channel to have a mutabelTime', async () => {
        const response = await hubClientPut(url, headers, channelBody);
        const body = getProp('body', response);
        expect(getProp('ttlDays', body)).toBe(0);
        expect(getProp('maxItems', body)).toBe(0);
        expect(getProp('mutableTime', body)).toBe(expected);
    });

    utils.itRefreshesChannels();

    utils.getChannel(channel, function (response) {
        const parse = utils.parseJson(response, __filename);
        expect(getProp('ttlDays', parse)).toBe(0);
        expect(getProp('maxItems', parse)).toBe(0);
        expect(getProp('mutableTime', parse)).toBe(expected);
    }, __filename);
});
