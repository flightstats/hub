require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
    hubClientGet,
    hubClientPut,
} = require('../lib/helpers');
const {
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const hubUrlBase = getHubUrlBase();
const channel = utils.randomChannelName();
const tag = Math.random().toString().replace(".", "");
const testName = __filename;
const channelBody = {
    tags: [tag, "test"],
    ttlDays: 1,
};
const headers = { 'Content-Type': 'application/json' };
let channelCreated = false;
/**
 * This should:
 * Create Channel with tag Tag
 *
 * Get all the tags and expect Channel to be in the list
 *
 * Get tag and make sure channel is in the list.
 *
 */
describe(testName, function () {
    const getAndMatch = async (url, nodeName, key) => {
        const name = key || tag;
        console.log('calling', url);
        const response = await hubClientGet(url, headers);
        const statusCode = getProp('statusCode', response);
        expect(statusCode).toBe(200);
        const body = getProp('body', response);
        const tags = fromObjectPath(['_links', nodeName], body) || [];
        const found = tags.find(tag => getProp('name', tag) === name);
        expect(getProp('name', found)).toBe(name);
    };
    beforeAll(async () => {
        const url = `${channelUrl}/${channel}`;
        const response = await hubClientPut(url, headers, channelBody);
        if (getProp('statusCode', response) === 201) {
            channelCreated = true;
        }
    });

    it('waits while the channel is refreshed', async () => {
        const response = await hubClientChannelRefresh();
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it(`get all tags ${tag}`, async () => {
        if (!channelCreated) return fail('channel not created in before block');
        await getAndMatch(`${hubUrlBase}/tag`, 'tags', tag);
    }, 60001);

    it(`gets tag ${tag}`, async () => {
        if (!channelCreated) return fail('channel not created in before block');
        await getAndMatch(`${hubUrlBase}/tag/${tag}`, 'channels', channel);
    }, 60001);
});
