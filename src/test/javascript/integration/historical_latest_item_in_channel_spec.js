require('../integration_config');
const rp = require('request-promise-native');
const moment = require('moment');
const {
    getProp,
    fromObjectPath,
    hubClientGet,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const mutableTime = moment.utc().subtract(1, 'minute');
const items = [];
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
let latest = null;
/**
 * create a channel
 * post 2 items
 * gets the item back out with latest
 * get both items back out with latest/10
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const historicalItem1 = `${channelResource}${moment(mutableTime).subtract(2, 'minute').format('/YYYY/MM/DD/HH/mm/ss/SSS')}`;
        const historicalItem2 = `${channelResource}${mutableTime.format('/YYYY/MM/DD/HH/mm/ss/SSS')}`;
        const response1 = await hubClientPostTestItem(historicalItem1);
        const response2 = await hubClientPostTestItem(historicalItem2);
        const actual = [response1, response2]
            .map(res => fromObjectPath(['headers', 'location'], res));
        items.push(...actual);
    });

    it("gets latest in default Epoch in channel ", async () => {
        const url = `${channelResource}/latest?trace=true`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets latest Immutable in channel ", async () => {
        const url = `${channelResource}/latest?epoch=IMMUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets latest Mutable in channel ", async () => {
        const url = `${channelResource}/latest?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(fromObjectPath(['headers', 'location'], response)).toEqual(items[1]);
        expect(getProp('statusCode', response)).toEqual(303);
    });

    it('posts item now', async () => {
        const response = await hubClientPostTestItem(channelResource);
        latest = fromObjectPath(['headers', 'location'], response);
        items.push(latest);
    });

    it("gets latest in Immutable in channel - after now item", async () => {
        const url = `${channelResource}/latest?stable=false`;
        const response = await hubClientGet(url, headers);
        expect(fromObjectPath(['headers', 'location'], response)).toEqual(latest);
        expect(getProp('statusCode', response)).toEqual(303);
    });

    it("gets latest Mutable in channel - after now item ", async () => {
        const url = `${channelResource}/latest?epoch=MUTABLE&trace=true`;
        const response = await hubClientGet(url, headers);
        expect(fromObjectPath(['headers', 'location'], response)).toEqual(items[1]);
        expect(getProp('statusCode', response)).toEqual(303);
    });

    it("gets latest N Mutable in channel ", async () => {
        try {
            const response = await rp({
                method: 'GET',
                headers,
                url: `${channelResource}/latest/10?epoch=MUTABLE`,
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toBe(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body) || [];
            expect(uris.length).toBeGreaterThan(0);
            const expected = items.slice(0, 2);
            const actual = uris
                .every((uri, index) => uri === expected[index]);
            expect(actual).toBe(true);
        } catch (ex) {
            console.log('gets latest N Mutable in channel failed', ex && ex.message);
            return fail(ex);
        }
    });

    it("gets latest N ALL in channel ", async () => {
        try {
            const response = await rp({
                method: 'GET',
                headers,
                url: `${channelResource}/latest/10?stable=false&epoch=ALL`,
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toBe(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body) || [];
            expect(uris.length).toBeGreaterThan(0);
            const actual = uris
                .every((uri, index) => uri === items[index]);
            expect(actual).toBe(true);
        } catch (ex) {
            console.log('gets latest N ALL in channel failed', ex && ex.message);
            return fail(ex);
        }
    });
});
