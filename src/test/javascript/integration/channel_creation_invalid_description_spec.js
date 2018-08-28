const {
    getProp,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();

describe(__filename, function () {
    it('creates a channel with an invalid description', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName, 'description': new Array(1026).join('a') };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
