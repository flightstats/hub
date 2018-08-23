const {
    fromObjectPath,
    getProp,
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

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with maxItems set', async () => {
        const maxItems = 50;
        const headers = { 'Content-Type': 'application/json' };
        const body = { 'name': channelName, 'maxItems': maxItems };

        const response = await hubClientPost(channelUrl, headers, body);
        const responseHeaders = getProp('headers', response);
        const responseBody = getProp('body', response);
        expect(getProp('statusCode', response)).toEqual(201);
        const [contentType, location] = ['content-type', 'location']
            .map(key => getProp(key, responseHeaders));
        const selfLink = fromObjectPath(['_links', 'self', 'href'], responseBody);
        const [
            name,
            description,
            replicationSource,
        ] = ['name', 'description', 'replicationSource']
            .map(key => getProp(key, responseBody));
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(location).toEqual(channelResource);
        expect(selfLink).toEqual(channelResource);
        expect(name).toEqual(channelName);
        expect(maxItems).toEqual(maxItems);
        expect(description).toEqual('');
        expect(replicationSource).toEqual('');
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(200);
    });
});
