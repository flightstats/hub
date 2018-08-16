require('../integration_config');
const { getProp, hubClientPost } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = '123_you_aint_gunna_find_me';
const channelResource = `${channelUrl}/${channelName}`;
const messageText = "Any old value!";

describe(__filename, function () {
    it('inserts an item into a bogus channel', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const response = await hubClientPost(channelResource, headers, messageText);
        expect(getProp('statusCode', response)).toEqual(404);
    });
});
