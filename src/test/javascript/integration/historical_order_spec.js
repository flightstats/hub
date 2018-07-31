require('../integration_config');
const moment = require('moment');
const { getProp, hubClientPut } = require('../lib/helpers');

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

    utils.addItem(`${channelResource}/2016/06/01/12/00/00/000`, 201);

    utils.addItem(`${channelResource}/2016/06/01/11/00/00/000`, 201);
});
