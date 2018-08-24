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
const headers = { 'Content-Type': 'application/json' };

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with a valid TTL', async () => {
        const body = { 'name': channelName, 'ttlMillis': 30000 };

        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        expect(contentType).toEqual('application/json');
        expect(ttlDays).toEqual(120);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        expect(contentType).toEqual('application/json');
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(name).toEqual(channelName);
        expect(ttlDays).toEqual(120);
    });
});
