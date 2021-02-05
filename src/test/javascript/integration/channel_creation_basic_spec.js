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

describe(__filename, function () {
    it('verifies the channel doesn\'t exist', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates the channel', async () => {
        var headers = { 'Content-Type': 'application/json' };
        var body = { 'name': channelName };

        const response = await hubClientPost(channelUrl, headers, body);
        const responseBody = getProp('body', response);
        expect(getProp('statusCode', response)).toEqual(201);
        const [contentType, location] = ['content-type', 'location']
            .map(key => response.header(key));
        const selfLink = fromObjectPath(['_links', 'self', 'href'], responseBody);
        const [
            name,
            ttlDays,
            description,
            replicationSource,
            storage,
        ] = ['name', 'ttlDays', 'description', 'replicationSource', 'storage']
            .map(key => getProp(key, responseBody));
        expect(contentType).toEqual('application/json');
        expect(location).toEqual(channelResource);
        expect(selfLink).toEqual(channelResource);
        expect(name).toEqual(channelName);
        expect(ttlDays).toEqual(120);
        expect(description).toEqual('');
        expect(replicationSource).toEqual('');
        expect(storage).toEqual('BATCH');
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(200);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
