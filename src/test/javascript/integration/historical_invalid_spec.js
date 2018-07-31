require('../integration_config');
const moment = require('moment');
const { getProp, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const mutableTime = moment.utc().subtract(1, 'minute');
/**
 * This should:
 * Create a normal channel
 * Verify no items can be inserted through the historical interface
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, {});
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.addItem(`${channelResource}/2016/06/01/12/00/00/000`, 403);

    utils.addItem(`${channelResource}/${moment.utc().format('YYYY/MM/DD/HH/mm/ss/SSS')}`, 403);

    // ({ "error":"Only one of ttlDays, maxItems and mutableTime can be defined " })
    it('returns a 400 on mutableTime PUT with bad configuration', async () => {
        const body = { mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS') };
        const response = await hubClientPut(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
