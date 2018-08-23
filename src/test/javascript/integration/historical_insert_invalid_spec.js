const moment = require('moment');
const {
    getProp,
    hubClientDelete,
    hubClientPut,
    hubClientPostTestItem,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = randomChannelName();
const tag = Math.random().toString().replace(".", "");
const mutableTime = moment.utc().subtract(2, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss'),
    tags: [tag, "test"],
};
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const afterMutableTimeUrl = `${channelResource}/${mutableTime.add(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS')}`;
/**
 * This should:
 * Create a channel with mutableTime
 * insert a historical item after the mutableTime
 */
describe(__filename, function () {
    it('creates a channel with mutableTime', async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('returns a 400 for item posted after the mutableTime', async () => {
        const response = await hubClientPostTestItem(afterMutableTimeUrl);
        expect(getProp('statusCode', response)).toEqual(400);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
