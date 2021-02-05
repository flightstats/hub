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
const channelResource = channelUrl + '/' + channelName;
const headers = { 'Content-Type': 'application/json' };

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with tags', async () => {
        const body = {
            'name': channelName,
            'tags': ['foo-bar', 'bar', 'tag:z'],
        };
        const response = await hubClientPost(channelUrl, headers, body);
        const contentType = response('content-type');
        const tags = fromObjectPath(['body', 'tags'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(tags).toEqual(['bar', 'foo-bar', 'tag:z']);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        const contentType = response('content-type');
        const tags = fromObjectPath(['body', 'tags'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(tags).toEqual(['bar', 'foo-bar', 'tag:z']);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
