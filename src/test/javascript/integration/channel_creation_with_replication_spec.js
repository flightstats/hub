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

    it('creates a channel with a replicationSource', async () => {
        const body = {
            'name': channelName,
            'replicationSource': 'http://hub/channel/nada',
        };
        const response = await hubClientPost(channelUrl, headers, body);
        const contentType = response('content-type');
        const replicationSource = fromObjectPath(['body', 'replicationSource'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(replicationSource).toEqual('http://hub/channel/nada');
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        const contentType = response('content-type');
        const replicationSource = fromObjectPath(['body', 'replicationSource'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(replicationSource).toEqual('http://hub/channel/nada');
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
