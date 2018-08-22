require('../integration_config');
const moment = require('moment');
const rp = require('request-promise-native');
const { getProp,
    fromObjectPath,
    hubClientChannelRefresh,
    hubClientPut,
    hubClientPostTestItem,
} = require('../lib/helpers');
const {
    getChannelUrl,
    getHubUrlBase,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const channel = utils.randomChannelName();
const channelResource = `${channelUrl}/${channel}`;
const mutableTime = moment.utc().subtract(1, 'day');
const headers = { 'Content-Type': 'application/json' };
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const channelBodyChange = {
    mutableTime: moment(mutableTime).subtract(1, 'day').format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};
const channelLocation = `${getHubUrlBase()}/channel/${channel}`;
const historicalLocations = [];
/**
 * This should:
 * Create a channel with mutableTime
 *
 * Put a historical item and one before that
 * Move the mutableTime before the oldest item
 * query latest with epochs
 */

describe(__filename, function () {
    it('creates a channel with mutableTime', async () => {
        const response = await hubClientPut(channelResource, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it(`posts historical item to ${channel}`, async () => {
        const url = `${channelLocation}/${moment(mutableTime).subtract(1, 'hour').format('YYYY/MM/DD/HH/mm/ss/SSS')}`;
        const response1 = await hubClientPostTestItem(url);
        const location = fromObjectPath(['headers', 'location'], response1);
        console.log('first - value.response.headers.location', location);
        historicalLocations.push(location);
        const url2 = `${channelLocation}/${moment(mutableTime).format('YYYY/MM/DD/HH/mm/ss/SSS')}`;
        const response2 = await hubClientPostTestItem(url2);
        const nextLocation = fromObjectPath(['headers', 'location'], response2);
        console.log('second - nextValue.response.headers.location', nextLocation);
        historicalLocations.push(nextLocation);
    });

    it('change the mutableTime backward', async () => {
        const response = await hubClientPut(channelResource, headers, channelBodyChange);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    it('waits while the channel is refreshed', async () => {
        const response = await hubClientChannelRefresh();
        expect(getProp('statusCode', response)).toEqual(200);
    });

    it('queries both items', async () => {
        try {
            const response = await rp({
                url: `${channelLocation}/latest/2?trace=true`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
            expect(getProp('statusCode', response)).toBe(200);
            const body = JSON.parse(getProp('body', response));
            const uris = fromObjectPath(['_links', 'uris'], body) || [];
            expect(uris.length).toBeGreaterThan(0);
            const actual = uris
                .every((uri, index) => uri === historicalLocations[index]);
            expect(actual).toBe(true);
        } catch (ex) {
            console.log('queries both items failed', ex && ex.message);
            return fail(ex);
        }
    });

    it('queries mutable items', async () => {
        try {
            await rp({
                url: `${channelLocation}/latest/2?trace=true&epoch=MUTABLE`,
                method: 'GET',
                resolveWithFullResponse: true,
            });
        } catch (ex) {
            expect(getProp('statusCode', ex)).toBe(404);
        }
    });
});
