require('../integration_config');
const moment = require('moment');
const { getProp, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const tag = Math.random().toString().replace(".", "");
const mutableTime = moment.utc().subtract(2, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss'),
    tags: [tag, "test"],
};
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const pointInThePastURL = `${channelResource}/${mutableTime.add(1, 'minute').format('YYYY/MM/DD/HH/mm/ss/SSS')}`;
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

    utils.addItem(pointInThePastURL, 400);
});
