require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPost,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
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
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const tags = fromObjectPath(['body', 'tags'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(tags).toEqual(['bar', 'foo-bar', 'tag:z']);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const tags = fromObjectPath(['body', 'tags'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(tags).toEqual(['bar', 'foo-bar', 'tag:z']);
    });
});
