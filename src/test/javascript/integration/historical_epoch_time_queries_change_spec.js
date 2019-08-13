const request = require('request');
const moment = require('moment');
const {
    fromObjectPath,
    getProp,
    hubClientChannelRefresh,
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
    storage: 'SINGLE',
};
let historicalLocation = null;
let liveLocation = null;
let liveTime = null;
/**
 * This should:
 * Create a channel
 * change the channel to have a mutableTime
 * insert an item before the mutableTime, verify item with get
 * insert an item into now, verify item with get
 * Query items by time, verify exclusion
 */
describe(__filename, () => {
    const timeQuery = (query, expected) => {
        const url = `${channelResource}${query}`;
        request.get({ url, json: true },
            (err, response, body) => {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const uris = fromObjectPath(['_links', 'uris'], body) || [];
                expect(uris.length).toBe(expected.length);
                for (let i = 0; i < uris.length; i++) {
                    expect(expected[i]).toBeDefined();
                    expect(uris[i]).toBe(expected[i]);
                }
            });
    };

    const queryTimes = (format) => {
        const liveQuery = liveTime.format(format);
        const mutableQuery = mutableTime.format(format);

        let queryAll = () => {};
        if (liveQuery === mutableQuery) {
            queryAll = () => {
                timeQuery(`${mutableQuery}?epoch=ALL&trace=true&stable=false`, [historicalLocation, liveLocation]);
            };
        }

        const queryMutable = () => {
            timeQuery(`${mutableQuery}?epoch=MUTABLE&trace=true&stable=false`, [historicalLocation], queryAll);
        };
        timeQuery(`${liveQuery}?epoch=IMMUTABLE&trace=true&stable=false`, [liveLocation], queryMutable);
    };

    beforeAll(async () => {
        const response = await hubClientPut(channelResource, headers, { ttlDays: 20 });
        expect(getProp('statusCode', response)).toEqual(201);
        const response2 = await hubClientChannelRefresh();
        expect(getProp('statusCode', response2)).toEqual(200);
    });

    it('updates the channel to mutableTime', async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
        const response2 = await hubClientChannelRefresh();
        expect(getProp('statusCode', response2)).toEqual(200);
    });

    it(`posts historical item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(pointInThePastURL);
        historicalLocation = fromObjectPath(['headers', 'location'], response);
    });

    it(`gets historical item from ${historicalLocation}`, () => {
        request.get({ url: historicalLocation },
            (err, response) => {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
            });
    });

    it(`posts live item to ${channel}`, async () => {
        const response = await hubClientPostTestItem(channelResource);
        liveLocation = fromObjectPath(['headers', 'location'], response);
        liveTime = moment((liveLocation || '').substring(channelResource.length), '/YYYY/MM/DD/HH/mm/ss/SSS');
    });

    it(`gets live item from ${liveLocation}`, () => {
        request.get({ url: liveLocation },
            (err, response) => {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
            });
    });

    it('mutable item by day', () => {
        queryTimes('/YYYY/MM/DD');
    });

    it('mutable item by hour', () => {
        queryTimes('/YYYY/MM/DD/HH');
    });

    it('mutable item by minute', () => {
        queryTimes('/YYYY/MM/DD/HH/mm');
    });

    it('mutable item by second', () => {
        queryTimes('/YYYY/MM/DD/HH/mm/ss');
    });

    it('mutable item by millis', () => {
        queryTimes('/YYYY/MM/DD/HH/mm/ss/SSS');
    });

    afterAll(async () => {
        await hubClientDelete(channelResource);
    });
});
