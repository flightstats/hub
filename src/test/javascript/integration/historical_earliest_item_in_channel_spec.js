require('../integration_config');
const rp = require('request-promise-native');
const {
    fromObjectPath,
    getProp,
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
const moment = require('moment');
const mutableTime = moment.utc().subtract(1, 'minute');
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const items = [];
/**
 * create a channel
 * post 2 historical items
 * gets the item back out with earliest
 * post a current item
 * get items back out with earliest/10
 */
describe(__filename, function () {
    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('posts two historical items', async () => {
        const historicalItem1 = `${channelResource}/2013/11/20/12/00/00/000`;
        const historicalItem2 = `${channelResource}/2013/11/20/12/01/00/000`;
        const response1 = await hubClientPostTestItem(historicalItem1);
        const location1 = fromObjectPath(['headers', 'location'], response1);
        items.push(location1);
        const response2 = await hubClientPostTestItem(historicalItem2);
        const nextLocation = fromObjectPath(['headers', 'location'], response2);
        items.push(nextLocation);
    });

    it("gets earliest in default Epoch in channel ", async () => {
        const url = `${channelResource}/earliest?trace=true`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest Immutable in channel missing ", async () => {
        const url = `${channelResource}/earliest?epoch=IMMUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(404);
    });

    it("gets earliest Mutable in channel", async () => {
        const url = `${channelResource}/earliest?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(303);
        expect(fromObjectPath(['headers', 'location'], response)).toEqual(items[0]);
    });

    it('posts item now', async () => {
        const response = await hubClientPostTestItem(channelResource);
        const location = fromObjectPath(['headers', 'location'], response);
        items.push(location);
    });

    it("gets earliest Immutable in channel - after now item", async () => {
        const url = `${channelResource}/earliest?stable=false&trace=true`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(303);
        expect(fromObjectPath(['headers', 'location'], response)).toEqual(items[2]);
    });

    it("gets earliest Mutable in channel - after now item", async () => {
        const url = `${channelResource}/earliest?epoch=MUTABLE`;
        const response = await hubClientGet(url, headers);
        expect(getProp('statusCode', response)).toEqual(303);
        expect(fromObjectPath(['headers', 'location'], response)).toEqual(items[0]);
    });

    it("gets earliest N Mutable in channel ", async () => {
        try {
            const response = await rp({
                method: 'GET',
                url: `${channelResource}/earliest/10?epoch=MUTABLE`,
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toBe(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body) || [];
            expect(uris.length).toBeGreaterThan(0);
            const expected = items.slice(0, 2);
            const actual = uris
                .every((uri, index) => uri && uri === expected[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed get earliest N in channel', ex && ex.message);
            return fail(ex);
        }
    });

    it("gets earliest N ALL in channel ", async () => {
        try {
            const response = await rp({
                method: 'GET',
                url: `${channelResource}/earliest/10?stable=false&epoch=ALL`,
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toBe(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body) || [];
            expect(uris.length).toBeGreaterThan(0);
            const actual = uris
                .every((uri, index) => uri && uri === items[index]);
            expect(actual).toEqual(true);
        } catch (ex) {
            console.log('failed get earliest N in channel', ex && ex.message);
            return fail(ex);
        }
    });
});
