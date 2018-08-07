require('../integration_config');
const {
    hubClientGet,
    fromObjectPath,
    getProp,
    hubClientPatch,
    hubClientPost,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
describe(__filename, function () {
    it('verifies the channel doesn\'t exist yet', async () => {
        const response = await hubClientGet(channelResource);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('creates the channel', async () => {
        const body = { 'name': channelName, 'ttlMillis': null };
        const response = await hubClientPost(channelUrl, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('updates the channel TTL', async () => {
        const body = { ttlMillis: null };
        const response = await hubClientPatch(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
    });
});
