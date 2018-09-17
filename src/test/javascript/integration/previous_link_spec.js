const {
    fromObjectPath,
    getProp,
    hubClientDelete,
    hubClientGet,
    hubClientPost,
    hubClientPut,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channelName = randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
const items = [];
const headers = { 'Content-Type': 'application/json' };
const body = { 'name': channelName };

describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { name: channelName, ttlDays: 1 });
        expect(getProp('statusCode', response)).toEqual(201);
    });

    const postOneItem = async () => {
        const response = await hubClientPost(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
        const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
        items.push(selfLink);
    };

    it(`posts item ${__filename}1`, async () => {
        await postOneItem();
    });

    it('gets 404 from /previous ', async () => {
        const response = await hubClientGet(`${items[0]}/previous`, headers, body);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('gets empty list from /previous/2 ', async () => {
        const response = await hubClientGet(`${items[0]}/previous/2`, headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response);
        const urisLength = !!uris && uris.length === 0;
        expect(urisLength).toBe(true);
    });

    it(`posts item ${__filename}2`, async () => {
        await postOneItem();
    });

    it(`posts item ${__filename}3`, async () => {
        await postOneItem();
    });

    it('gets item from /previous ', async () => {
        const response = await hubClientGet(`${items[2]}/previous?stable=false`, headers, body);
        expect(getProp('statusCode', response)).toEqual(303);
        const location = fromObjectPath(['headers', 'location'], response);
        expect(location).toBe(items[1]);
    });

    it('gets items from /previous/2 ', async () => {
        const response = await hubClientGet(`${items[2]}/previous/2?stable=false`, headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        expect(uris.length).toBe(2);
        expect(uris[0]).toBe(items[0]);
        expect(uris[1]).toBe(items[1]);
    });

    it('gets inclusive items from /previous/2 ', async () => {
        const response = await hubClientGet(`${items[2]}/previous/2?stable=false&inclusive=true`, headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        expect(uris.length).toBe(2);
        expect(uris[0]).toBe(items[1]);
        expect(uris[1]).toBe(items[2]);
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
