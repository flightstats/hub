const {
    getProp,
    fromObjectPath,
    hubClientDelete,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();

const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
/**
 * create a channel via put with protect == true
 * verify that we can not change some settings (storage to BATCH)
 * should not be able to change protect to false
 *
 */
const url = `${channelUrl}/${channelName}`;
describe(__filename, function () {
    it('creates an unprotected channel', async () => {
        const response = await hubClientPut(url, headers, { tags: ['one', 'two'] });
        const body = getProp('body', response);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], body);
        expect(selfLink).toEqual(channelResource);
        expect(getProp('ttlDays', body)).toEqual(120);
        expect(getProp('description', body)).toEqual('');
        expect((getProp('tags', body) || '').length).toEqual(2);
        expect(getProp('storage', body)).toEqual('BATCH');
        expect(getProp('protect', body)).toEqual(false);
    });

    it('sets the channel to protected', async () => {
        const response = await hubClientPut(url, headers, { protect: true, owner: 'someone' });
        const body = getProp('body', response);
        const selfLink = fromObjectPath(['_links', 'self', 'href'], body);
        expect(selfLink).toEqual(channelResource);
        expect(getProp('protect', body)).toEqual(true);
    });

    it('returns 403 failure on attempt to unprotect the channel', async () => {
        const response = await hubClientPut(url, headers, { protect: false });
        expect(getProp('statusCode', response)).toEqual(403);
    });

    it('returns 403 failure on attempt to remove original storage source on protected channel', async () => {
        const response = await hubClientPut(url, headers, { storage: 'BATCH' });
        expect(getProp('statusCode', response)).toEqual(403);
    });

    it('returns 403 failure on attempt to remove tags on protected channel', async () => {
        const response = await hubClientPut(url, headers, { tags: ['one'] });
        expect(getProp('statusCode', response)).toEqual(403);
    });

    it('returns 403 failure on attempt to decrease ttl on protected channel', async () => {
        const response = await hubClientPut(url, headers, { ttlDays: 119 });
        expect(getProp('statusCode', response)).toEqual(403);
    });

    it('returns 403 failure on attempt to change owner on protected channel', async () => {
        const response = await hubClientPut(url, headers, { owner: 'CBA' });
        expect(getProp('statusCode', response)).toEqual(403);
    });

    it('successfully updates tags and storage additional to original ones', async () => {
        const requestBody = { storage: 'BOTH', tags: ['one', 'two', 'three'] };
        const response = await hubClientPut(url, headers, requestBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('returns a 403 to delete protected channel from external', async () => {
        const response = await hubClientDelete(channelResource);
        expect(getProp('statusCode', response)).toBe(403);
    });
});
