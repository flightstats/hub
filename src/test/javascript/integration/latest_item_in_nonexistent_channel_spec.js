require('../integration_config');
const { getProp, hubClientGet } = require('../lib/helpers');
const channelName = utils.randomChannelName();
const channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {
    it('verifies the latest endpoint returns 404 on a nonexistent channel', async () => {
        const url = `${channelResource}/latest`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
