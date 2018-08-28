const moment = require('moment');
const { getProp, hubClientDelete, hubClientPut, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = randomChannelName();
const url = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const requestBody = {
    tags: ["test"],
};
/**
 * This should:
 * Create a channel with mutableTime
 *
 * Attempt to move the mutableTime forward
 */
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

    afterAll(async () => {
        await hubClientDelete(`${channelUrl}/${channel}`);
    });
});
