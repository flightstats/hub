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

    it('creates a channel with an owner', async () => {
        const body = {
            'name': channelName,
            'owner': 'pwned',
        };
        const response = await hubClientPost(channelUrl, headers, body);
        const contentType = response('content-type');
        const owner = fromObjectPath(['body', 'owner'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(owner).toEqual('pwned');
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        const contentType = response('content-type');
        const name = fromObjectPath(['body', 'name'], response);
        const owner = fromObjectPath(['body', 'owner'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(owner).toEqual('pwned');
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
