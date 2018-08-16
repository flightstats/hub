require('../integration_config');
const moment = require('moment');
const {
    getProp,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = utils.randomChannelName();
const mutableTime = moment.utc().subtract(1, 'minute');
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
/**
 * This should:
 * Create a historical channel
 * Add an item
 * Add an item before the first
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts an item successfully to a histrocal channel', async () => {
        const response = await hubClientPostTestItem(`${channelResource}/2016/06/01/12/00/00/000`);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts a histrocal item before the first successfully to a histrocal channel', async () => {
        const response = await hubClientPostTestItem(`${channelResource}/2016/06/01/11/00/00/000`);
        expect(getProp('statusCode', response)).toEqual(201);
    });
});
