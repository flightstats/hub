const {
    createChannel,
    fromObjectPath,
    getProp,
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
let createdChannel = false;

describe(__filename, function () {
    beforeAll(async () => {
        const channel = await createChannel(channelName);
        if (getProp('statusCode', channel)) {
            createdChannel = true;
            console.log(`created channel for ${__filename}`);
        }
    });

    const items = [];
    const headers = { 'Content-Type': 'application/json' };
    const body = { 'name': channelName };

    const postOneItem = async () => {
        const response = await hubClientPost(channelResource, headers, body);
        expect(getProp('statusCode', response)).toEqual(201);
        const selfLink = fromObjectPath(['body', '_links', 'self', 'href'], response);
        items.push(selfLink);
    };

    it(`posts item ${__filename}1`, async () => {
        await postOneItem();
    });

    it('gets 404 from /next ', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientGet(items[0] + '/next', headers, body);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it('gets empty list from /next/2 ', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientGet(items[0] + '/next/2', headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response);
        const urisLength = !!uris && uris.length === 0;
        expect(urisLength).toBe(true);
    });

    it(`posts item ${__filename}2`, async () => {
        if (!createdChannel) return fail('channel not created in before block');
        await postOneItem();
    });

    it(`posts item ${__filename}3`, async () => {
        if (!createdChannel) return fail('channel not created in before block');
        await postOneItem();
    });

    it('gets item from /next ', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientGet(items[0] + '/next?stable=false', headers, body);
        expect(getProp('statusCode', response)).toEqual(303);
        const location = fromObjectPath(['headers', 'location'], response);
        expect(location).toBe(items[1]);
    });

    it('gets items from /next/2 ', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientGet(items[0] + '/next/2?stable=false', headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        expect(uris.length).toBe(2);
        expect(uris[0]).toBe(items[1]);
        expect(uris[1]).toBe(items[2]);
    });

    it('gets inclusive items from /next/2 ', async () => {
        if (!createdChannel) return fail('channel not created in before block');
        const response = await hubClientGet(items[0] + '/next/2?stable=false&inclusive=true', headers, body);
        expect(getProp('statusCode', response)).toEqual(200);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        expect(uris.length).toBe(2);
        expect(uris[0]).toBe(items[0]);
        expect(uris[1]).toBe(items[1]);
    });
});
