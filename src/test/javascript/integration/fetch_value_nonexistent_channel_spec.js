require('../integration_config');
const { getProp, hubClientGet } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('verifies a 404 is returned on a nonexistent channel', async () => {
        const url = `${channelResource}/2014/12/31/23/59/59/999/685221b0-77c2-11e2-8a3e-20c9d08600a5`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
