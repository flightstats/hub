require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');

var channelName = utils.randomChannelName();

describe(__filename, function () {
    it('creates a channel with an invalid description', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName, 'description': new Array(1026).join('a') };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
