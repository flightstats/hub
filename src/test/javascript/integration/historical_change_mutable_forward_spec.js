require('../integration_config');
const { getProp, hubClientPut } = require('../lib/helpers');
const channel = utils.randomChannelName();
const moment = require('moment');
/**
 * This should:
 * Create a channel with mutableTime
 *
 * Attempt to move the mutableTime forward
 */
const url = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const requestBody = {
    tags: ["test"],
};
describe(__filename, function () {
    it('creates a channel with mutableTime', async () => {
        requestBody.mutableTime = moment.utc().subtract(1, 'day').format('YYYY-MM-DDTHH:mm:ss.SSS');
        const response = await hubClientPut(url, headers, requestBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('returns a 400 response code when attempt to change the mutableTime forward', async () => {
        requestBody.mutableTime = moment.utc().subtract(1, 'hour').format('YYYY-MM-DDTHH:mm:ss.SSS');
        const response = await hubClientPut(url, headers, requestBody);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
