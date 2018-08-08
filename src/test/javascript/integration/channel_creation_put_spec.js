require('../integration_config');
const { getProp, fromObjectPath, hubClientPut } = require('../lib/helpers');

const headers = { 'Content-Type': 'application/json' };
const getParsedPropFunc = parsed => prop => getProp(prop, parsed);
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
/**
 * create a channel via put
 * verify that it exists
 * change the channel via put
 * verify that the new config exists
 */
describe(__filename, function () {
    let returnedBody;
    it('creates a channel with method PUT', async () => {
        const response = await hubClientPut(channelResource, headers, {});
        const body = getProp('body', response);
        returnedBody = body;
        expect(fromObjectPath(['_links', 'self', 'href'], body)).toEqual(channelResource);
        const getParsedProp = getParsedPropFunc(body);
        expect(fromObjectPath(['_links', 'self', 'href'], body)).toEqual(channelResource);
        expect(getParsedProp('ttlDays')).toEqual(120);
        expect(getParsedProp('description')).toEqual('');
        expect((getParsedProp('tags') || '').length).toEqual(0);
        expect(getParsedProp('replicationSource')).toEqual('');
    });

    const newConfig = {
        description: 'yay put!',
        ttlDays: 5,
        tags: ['one', 'two'],
    };

    it('updates the channel config with method PUT', async () => {
        const response = await hubClientPut(channelResource, headers, newConfig);
        const body = getProp('body', response);
        expect(fromObjectPath(['_links', 'self', 'href'], body)).toEqual(channelResource);
        const getParsedProp = getParsedPropFunc(body);
        expect(getParsedProp('ttlDays')).toEqual(newConfig.ttlDays);
        expect(getParsedProp('description')).toEqual(newConfig.description);
        expect(getParsedProp('tags')).toEqual(newConfig.tags);
        expect(getParsedProp('creationDate')).toEqual(returnedBody.creationDate);
    });
});
