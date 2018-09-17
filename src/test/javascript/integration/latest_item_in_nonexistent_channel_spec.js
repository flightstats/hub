const { getProp, hubClientGet, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('verifies the latest endpoint returns 404 on a nonexistent channel', async () => {
        const url = `${channelResource}/latest`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
