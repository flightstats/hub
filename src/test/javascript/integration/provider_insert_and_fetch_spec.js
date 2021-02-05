const {
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPost,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const providerResource = `${getHubUrlBase()}/provider`;
const channelResource = `${channelUrl}/${channelName}`;
const messageText = `MY SUPER TEST CASE: this & <that>.${Math.random()}`;

describe(__filename, function () {
    it('inserts a value into a provider channel', async () => {
        const headers = {
            'channelName': channelName,
            'Content-Type': 'text/plain',
        };
        const response = await hubClientPost(providerResource, headers, messageText);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('verifies the value was inserted', async () => {
        const url = `${channelResource}/latest?stable=false`;
        const res = await hubClientGet(url);
        const response = await followRedirectIfPresent(res);
        const contentType = response('content-type');
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('text/plain');
        expect(getProp('body', response)).toContain(messageText);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
