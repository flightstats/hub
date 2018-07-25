require('../integration_config');
const { getProp, hubClientGet } = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {
    it('verifies the earliest endpoint returns 404 on a nonexistent channel', async () => {
        const url = `${channelResource}/earliest`;
        const response = await hubClientGet(url);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
