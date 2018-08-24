require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();

describe(__filename, function () {
    it('creates a channel with an invalid tag', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName, 'tags': ['foo bar', 'bar@home', 'tagz'] };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
