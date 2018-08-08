require('../integration_config');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');

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

    it('returns a 403 attempt to insert item on historical interface without mutableTime', async () => {
        const response = await hubClientPostTestItem(`${channelResource}/2016/06/01/12/00/00/000`);
        expect(getProp('statusCode', response)).toEqual(403);
        expect(getProp('body', response)).toEqual('historical inserts require a mutableTime on the channel.');
    });

    it('returns a 403 attempt to insert item on historical interface without mutableTime', async () => {
        const response = await hubClientPostTestItem(`${channelResource}/${moment.utc().format('YYYY/MM/DD/HH/mm/ss/SSS')}`);
        expect(getProp('statusCode', response)).toEqual(403);
        expect(getProp('body', response)).toEqual('historical inserts require a mutableTime on the channel.');
    });

    it('returns a 400 on mutableTime PUT with bad configuration', async () => {
        const body = { mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS') };
        const response = await hubClientPut(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
        expect(fromObjectPath(['body', 'error'], response))
            .toContain('Only one of ttlDays, maxItems and mutableTime can be defined');
    });
});
