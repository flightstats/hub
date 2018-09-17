const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPostTestItem,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
let url;
let createdChannel = false;
const headers = {
    'Accept': 'text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2',
};
describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
        }
        const json = { 'Content-Type': 'application/json' };
        const response = await hubClientPostTestItem(channelResource, json);
        url = fromObjectPath(['body', '_links', 'self', 'href'], response);
    });

    it('checks accept header', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toBe(200);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
