const {
    fromObjectPath,
    getProp,
    hubClientDelete,
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
    it('creates a channel with whitespace in the name', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { name: `    ${channelName}    ` };

        const response = await hubClientPost(channelUrl, headers, body);
        const contentType = response('content-type');
        const location = response.header('location');;
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(location).toEqual(channelResource);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
