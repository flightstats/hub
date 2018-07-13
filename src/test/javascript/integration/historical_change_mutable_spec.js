require('../integration_config');
const { fromObjectPath } = require('../lib/helpers');

var channel = utils.randomChannelName();
var moment = require('moment');

var testName = __filename;

/**
 * This should:
 * Create a channel with mutableTime
 *
 * Put a historical item and one before that
 * Move the mutableTime before the oldest item
 * query latest with epochs
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'day');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var channelURL = hubUrlBase + '/channel/' + channel;
    var historicalLocations = [];

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

    var channelBodyChange = {
        mutableTime: moment(mutableTime).subtract(1, 'day').format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBodyChange, testName);

    utils.itRefreshesChannels();

    it('queries both items', function (done) {
        utils.getQuery(channelURL + '/latest/2?trace=true', 200, historicalLocations, done);
    });

    it('queries mutable items', function (done) {
        utils.getQuery(channelURL + '/latest/2?trace=true&epoch=MUTABLE', 404, false, done);
    });

});
