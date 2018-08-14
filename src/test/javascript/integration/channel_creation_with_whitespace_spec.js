require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientPost,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

describe(__filename, function () {
    it('creates a channel with whitespace in the name', async () => {
        const headers = { 'Content-Type': 'application/json' };
        const body = { name: `    ${channelName}    ` };

        const response = await hubClientPost(channelUrl, headers, body);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const location = fromObjectPath(['headers', 'location'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(location).toEqual(channelResource);
    });
});
