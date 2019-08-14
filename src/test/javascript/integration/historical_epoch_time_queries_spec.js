const request = require('request');
const moment = require('moment');
const {
    followRedirectIfPresent,
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
    hubClientGet,
    hubClientDelete,
    hubClientPut,
    hubClientPostTestItem,
    randomChannelName,
} = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = randomChannelName();
const tag = Math.random().toString().replace('.', '');
const mutableTime = moment.utc().subtract(1, 'minute');
const channelResource = `${channelUrl}/${channel}`;
const headers = { 'Content-Type': 'application/json' };
const pointInThePastURL = `${channelResource}/${mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS')}`;
const channelBody = {
    ttlDays: 0,
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: [tag, 'test'],
};
let historicalLocation = null;
let liveLocation = null;
let liveTime = null;
/**
 * This should:
 * Create a channel with mutableTime
 * insert an item before the mutableTime, verify item with get
 * insert an item into now, verify item with get
 * Query items by time, verify exclusion
 */
describe(__filename, () => {
    const timeQuery = async (query, expected, callbacks) => {
        const url = `${channelResource}${query}`;
        const originalResponse = await hubClientGet(url, headers);
        const response = await followRedirectIfPresent(originalResponse, headers);
        const uris = fromObjectPath(['body', '_links', 'uris'], response) || [];
        expect(uris.length).toBe(expected.length);
        for (let i = 0; i < uris.length; i++) {
            expect(expected[i]).toBeDefined();
            expect(uris[i]).toBe(expected[i]);
        }
        await Promise.all(callbacks.map(async cb => cb()));
    };

    const queryTimes = async (format) => {
        const liveQuery = liveTime.format(format);
        const mutableQuery = mutableTime.format(format);
        const callbacks = [];

        if (liveQuery === mutableQuery) {
            const queryAll = async () => timeQuery(`${mutableQuery}?epoch=ALL&trace=true&stable=false`, [historicalLocation, liveLocation], []);
            callbacks.push(queryAll);
        }

        const queryMutable = async () => timeQuery(`${mutableQuery}?epoch=MUTABLE&trace=true&stable=false`, [historicalLocation], []);
        callbacks.push(queryMutable);
        await timeQuery(`${liveQuery}?epoch=IMMUTABLE&trace=true&stable=false`, [liveLocation], callbacks);
    };

    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const response2 = await hubClientChannelRefresh();
        expect(getProp('statusCode', response2)).toEqual(200);
    });

    it(`posts historical item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(pointInThePastURL);
        historicalLocation = fromObjectPath(['headers', 'location'], response);
    });

    it(`gets historical item from ${historicalLocation}`, async () => {
        const response = await hubClientGet(historicalLocation, headers);
        expect(getProp('statusCode', response)).toBe(200);
    });

    it(`posts live item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(channelResource);
        liveLocation = fromObjectPath(['headers', 'location'], response);
        liveTime = moment((liveLocation || '').substring(channelResource.length), '/YYYY/MM/DD/HH/mm/ss/SSS');
    });

    it(`gets live item from ${liveLocation}`, async () => {
        const response = await hubClientGet(liveLocation, headers);
        expect(getProp('statusCode', response)).toBe(200);
    });

    it('mutable item by day', async () => queryTimes('/YYYY/MM/DD'));

    it('mutable item by hour', async () => queryTimes('/YYYY/MM/DD/HH'));

    it('mutable item by minute', async () => queryTimes('/YYYY/MM/DD/HH/mm'));

    it('mutable item by second', async () => queryTimes('/YYYY/MM/DD/HH/mm/ss'));

    it('mutable item by millis', async () => queryTimes('/YYYY/MM/DD/HH/mm/ss/SSS'));

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
