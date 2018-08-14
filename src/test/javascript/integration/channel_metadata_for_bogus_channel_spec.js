require('../integration_config');
const { getProp, hubClientGet } = require('../lib/helpers');

const channelName = "no_way_jose90928280xFF";
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('gets 404 on channel metadata request for a nonexistent channel', async () => {
        const headers = {};
        const body = '';
        const response = await hubClientGet(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
