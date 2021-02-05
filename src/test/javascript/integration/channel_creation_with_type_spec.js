const {
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
const headers = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with valid storage', async () => {
        const body = { 'name': channelName, 'storage': 'BOTH' };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
        const contentType = response('content-type');
        const storage = fromObjectPath(['body', 'storage'], response);
        expect(contentType).toEqual('application/json');
        expect(storage).toEqual('BOTH');
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = response('content-type');
        const storage = fromObjectPath(['body', 'storage'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(storage).toEqual('BOTH');
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
