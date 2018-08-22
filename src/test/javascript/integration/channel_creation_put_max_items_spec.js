require('../integration_config');
const { fromObjectPath, getProp, hubClientPut } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
/**
 * create a channel via put
 * verify that it exists
 * change the channel via put
 * verify that the new config exists
 */
describe(__filename, function () {
    const firstConfig = {
        ttlDays: 120,
    };

    it('creates a channel with method PUT', async () => {
        const response = await hubClientPut(channelResource, headers, firstConfig);
        const body = getProp('body', response);
        const getParsedProp = prop => getProp(prop, body);
        expect(fromObjectPath(['_links', 'self', 'href'], body)).toEqual(channelResource);
        expect(getParsedProp('ttlDays')).toEqual(120);
        expect(getParsedProp('maxItems')).toEqual(0);
        expect(getParsedProp('replicationSource')).toEqual('');
    });

    const newConfig = {
        ttlDays: 0,
        maxItems: 100,
    };

    it('updates the channel config with method PUT', async () => {
        const response = await hubClientPut(channelResource, headers, newConfig);
        const body = getProp('body', response);
        const getParsedProp = prop => getProp(prop, body);
        expect(fromObjectPath(['_links', 'self', 'href'], body)).toEqual(channelResource);
        expect(getParsedProp('ttlDays')).toEqual(0);
        expect(getParsedProp('maxItems')).toEqual(100);
    });
});
