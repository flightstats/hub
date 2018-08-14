require('../integration_config');
const { getProp, fromObjectPath, hubClientPut } = require('../lib/helpers');

const channel = utils.randomChannelName();
const moment = require('moment');

/**
 * This should:
 * Create a channel with mutableTime
 *
 * Put a historical item and one before that
 * Move the mutableTime before the oldest item
 * query latest with epochs
 */
const mutableTime = moment.utc().subtract(1, 'day');
const headers = { 'Content-Type': 'application/json' };
const channelBody = {
    mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
    tags: ["test"],
};

describe(__filename, function () {
    it('creates a channel with mutableTime', async () => {
        const response = await hubClientPut(`${channelUrl}/${channel}`, headers, channelBody);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    const channelURL = `${hubUrlBase}/channel/${channel}`;
    const historicalLocations = [];

    it('posts historical item to ' + channel, function (done) {
        utils.postItemQ(channelURL + '/' + moment(mutableTime).subtract(1, 'hour').format('YYYY/MM/DD/HH/mm/ss/SSS'))
            .then(function (value) {
                const location = fromObjectPath(['response', 'headers', 'location'], value);
                console.log('uno - value.response.headers.location', location);
                historicalLocations.push(location);
                return utils.postItemQ(channelURL + '/' + mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS'));
            })
            .then(function (nextValue) {
                const nextLocation = fromObjectPath(['response', 'headers', 'location'], nextValue);
                console.log('due - nextValue.response.headers.location', nextLocation);
                historicalLocations.push(nextLocation);
                done();
            });
    });

    const channelBodyChange = {
        mutableTime: moment(mutableTime).subtract(1, 'day').format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"],
    };

    it('change the mutableTime backward', async () => {
        const response = await hubClientPut(`${channelUrl}/${channel}`, headers, channelBodyChange);
        expect(getProp('statusCode', response)).toEqual(201);
    });

    utils.itRefreshesChannels();

    it('queries both items', function (done) {
        utils.getQuery(channelURL + '/latest/2?trace=true', 200, historicalLocations, done);
    });

    it('queries mutable items', function (done) {
        utils.getQuery(channelURL + '/latest/2?trace=true&epoch=MUTABLE', 404, false, done);
    });
});
