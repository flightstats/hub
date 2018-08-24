const { getProp, hubClientPost, randomChannelName } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();

describe(__filename, function () {
    it('creates a channel with an invalid tag', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName, 'tags': ['foo bar', 'bar@home', 'tagz'] };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(400);
    });
});
