require('../integration_config');
const rp = require('request-promise-native');
const {
    createChannel,
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPostTestItem,
} = require('../lib/helpers');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
let createdChannel = false;
let itemHref;

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel) === 201) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    it('adds item and checks relative links', async () => {
        const response = await hubClientPostTestItem(channelResource);
        itemHref = fromObjectPath(['body', '_links', 'self', 'href'], response);
        if (!createdChannel) return fail('channel not created in before block');
        const response2 = await hubClientGet(`${itemHref}/next/10?stable=false`, headers);
        const links = fromObjectPath(['body', '_links'], response2) || {};
        const { previous = {}, uris } = links;
        const urisLength = uris && uris.length === 0;
        expect(urisLength).toBe(true);
        expect(previous.href).toBeDefined();
        expect(previous.href).toBe(`${itemHref}/previous/10?stable=false`);
    });
});
