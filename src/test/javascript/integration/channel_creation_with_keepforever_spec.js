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
const headers = {'Content-Type': 'application/json'};
describe(__filename, function () {
    it('creates a channel with keepForever', async () => {
        const body = {
            'name': channelName,
            'keepForever': true,
        };
        const response = await hubClientPost(channelUrl, headers, body);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        const keepForever = fromObjectPath(['body', 'keepForever'], response);
        expect(getProp('statusCode', response)).toEqual(201);
        expect(contentType).toEqual('application/json');
        expect(ttlDays).toEqual(0);
        expect(keepForever).toEqual(true);
    });

    it('verifies the channel does exist', async () => {
        const response = await hubClientGet(channelResource, headers);
        const contentType = fromObjectPath(['headers', 'content-type'], response);
        const ttlDays = fromObjectPath(['body', 'ttlDays'], response);
        const keepForever = fromObjectPath(['body', 'keepForever'], response);
        const name = fromObjectPath(['body', 'name'], response);
        expect(getProp('statusCode', response)).toEqual(200);
        expect(contentType).toEqual('application/json');
        expect(name).toEqual(channelName);
        expect(ttlDays).toEqual(0);
        expect(keepForever).toEqual(true);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
