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
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const messageText = `MY SUPER TEST CASE: this & <that>. ${Math.random()}`;
const defaultHeaders = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    it('creates a channel', async () => {
        const body = { 'name': channelName };
        const response = await hubClientPost(channelUrl, defaultHeaders, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('inserts an item into the channel', async () => {
        const headers = { 'Content-Type': 'text/plain' };
        const response = await hubClientPost(channelResource, headers, messageText);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('verifies the channel metadata is accurate', async () => {
        const url = `${channelResource}/`;
        const res = await hubClientGet(url, defaultHeaders);
        const response = await followRedirectIfPresent(res, defaultHeaders);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const latestLInk = fromObjectPath(['body', '_links', 'latest', 'href'], response);
        const name = fromObjectPath(['body', 'name'], response);
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        expect(contentType).toEqual('application/json');
        expect(latestLInk).toEqual(`${channelResource}/latest`);
        expect(name).toEqual(channelName);
        expect(ttlDays).toEqual(120);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
