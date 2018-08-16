require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
describe(__filename, function () {
    it('creates a channel with no payload', async () => {
        const headers = {'Content-Type': 'application/json'};
        const body = '';
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
