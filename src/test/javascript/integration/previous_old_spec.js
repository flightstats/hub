require('../integration_config');
const {
    fromObjectPath,
    getProp,
    hubClientGet,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');
const rp = require('request-promise-native');

const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const headers = { 'Content-Type': 'application/json' };
const items = [];

describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);

        const postValue = await hubClientPostTestItem(channelResource);
        const href = fromObjectPath(['body', '_links', 'self', 'href'], postValue);
        items.push(href);
        console.log('href', href);

        const postValue2 = await hubClientPostTestItem(channelResource);
        const href2 = fromObjectPath(['body', '_links', 'self', 'href'], postValue2);
        items.push(href2);
        console.log('href', href2);

        const postValue3 = await hubClientPostTestItem(channelResource);
        const href3 = fromObjectPath(['body', '_links', 'self', 'href'], postValue3);
        items.push(href3);
        console.log('href', href3);
    });

    const getItem = async (url, status) => {
        const response = await hubClientGet(`${url}?stable=false`, headers);
        const statusCode = status || 200;
        expect(getProp('statusCode', response)).toBe(statusCode);
        return response;
    };

    it('404 on /previous with /N not supplied and service does not have resource', async () => {
        await getItem(items[0]);
        await getItem(`${items[0]}/previous`, 404);
    });

    it('200 on /previous with /N supplied and service does not have resource', async () => {
        const nextValue2 = await getItem(`${items[0]}/previous/2`);
        const uris = fromObjectPath(['body', '_links', 'uris'], nextValue2);
        const urisLength = !!uris && uris.length === 0;
        expect(urisLength).toBe(true);
    });

    it('303 ~> 200 on /previous with /N not supplied and service does have resource', async () => {
        /*
          TODO: this technically is a 303, request module auto follows redirects so we never see it
          is that the expected behavior?
        */
        try {
            const response = await rp({
                url: `${items[2]}/previous?stable=false`,
                headers,
                resolveWithFullResponse: true,
                json: true,
            });
            expect(getProp('statusCode', response)).toBe(200);
            const href = fromObjectPath(['request', 'href'], response);
            expect(href).toContain(items[1]);
        } catch (ex) {
            return fail(ex);
        }
    });

    it('gets and verifies both items from /previous/2', async () => {
        const value = await getItem(`${items[2]}/previous/2`, 200);
        const href = fromObjectPath(['body', '_links', 'previous', 'href'], value);
        const uris = fromObjectPath(['body', '_links', 'uris'], value) || [];
        expect(uris.length).toBe(2);
        expect(uris[0]).toBe(items[0]);
        expect(uris[1]).toBe(items[1]);
        expect(href).toBe(`${items[0]}/previous/2?stable=false`);
    });
});
