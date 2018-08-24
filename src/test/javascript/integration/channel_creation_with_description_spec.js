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
var channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };

describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates a channel with a description', async () => {
        const body = {
            'name': channelName,
            'description': 'describe me',
        };
        const response = await hubClientPost(channelUrl, headers, body);
        const description = fromObjectPath(['body', 'description'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(description).toEqual('describe me');
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        expect(getProp('statusCode', response)).toEqual(200);
        const description = fromObjectPath(['body', 'description'], response);
        const name = fromObjectPath(['body', 'name'], response);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(description).toEqual('describe me');
    });
});
