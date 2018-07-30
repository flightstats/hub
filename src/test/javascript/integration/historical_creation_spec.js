require('../integration_config');
const moment = require('moment');
const { getProp, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const tag = Math.random().toString().replace(".", "");
const headers = { 'Content-Type': 'application/json' };
const url = `${channelUrl}/${channel}`;
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
        const response = await hubClientPut(url, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const body = getProp('body', response);
        expect(getProp('ttlDays', body)).toBe(0);
        expect(getProp('maxItems', body)).toBe(0);
        expect(getProp('mutableTime', body)).toBe(expected);
    });

    utils.getChannel(channel, function (response) {
        const parse = utils.parseJson(response, __filename);
        expect(getProp('ttlDays', parse)).toBe(0);
        expect(getProp('maxItems', parse)).toBe(0);
        expect(getProp('mutableTime', parse)).toBe(expected);
    }, __filename);
});
