require('../integration_config');
const moment = require('moment');
const { getProp, fromObjectPath, hubClientPut, hubClientPostTestItem } = require('../lib/helpers');
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
const channelLocation = `${hubUrlBase}/channel/${channel}`;
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

    utils.itRefreshesChannels();

    it('queries both items', function (done) {
        utils.getQuery(`${channelLocation}/latest/2?trace=true`, 200, historicalLocations, done);
    });

    it('queries mutable items', function (done) {
        utils.getQuery(`${channelLocation}/latest/2?trace=true&epoch=MUTABLE`, 404, false, done);
    });
});
