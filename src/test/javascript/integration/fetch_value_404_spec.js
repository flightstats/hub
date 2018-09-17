const { getProp, hubClientGet, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('verifies a 404 is returned on a nonexistent item', async () => {
        const url = `${channelResource}/2014/12/31/23/59/59/999/foooo${Math.random()}`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
