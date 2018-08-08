require('../integration_config');
const { getProp, hubClientPut } = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('creates a channel with no information', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const response = await hubClientPut(channelResource, headers, {});
        expect(getProp('statusCode', response)).toEqual(201);
    });
});
