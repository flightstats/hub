require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var channel = utils.randomChannelName();
var moment = require('moment');

var testName = __filename;

/**
 * This should:
 * Create a channel with mutableTime
 * insert an item before the mutableTime
 * delete the item
 * confirm 404 with get
 *
 * insert a live item
 * live item deletion fails
 * live item still exists
 *
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var channelURL = hubUrlBase + '/channel/' + channel;
    var pointInThePastURL = channelURL + '/' + mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS');

    var historicalLocation;

    it('posts historical item to ' + channel, function (done) {
        utils.postItemQ(pointInThePastURL)
            .then(function (value) {
                historicalLocation = fromObjectPath(['response', 'headers', 'location'], value);
                done();
            });
    });

    it('deletes historical item ', function (done) {
        console.log('historicalLocation', historicalLocation);
        request.del({url: historicalLocation + '?trace=true'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(204);
                done();
            });
    });

    it('gets historical item ', function (done) {
        request.get({url: historicalLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });

    var liveLocation;

    it('posts live item to ' + channel, function (done) {
        utils.postItemQ(channelURL)
            .then(function (value) {
                liveLocation = fromObjectPath(['response', 'headers', 'location'], value);
                done();
            });
    });

    it('deletes live item ', function (done) {
        console.log('liveLocation', liveLocation);
        request.del({url: liveLocation + '?trace=true'},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(405);
                done();
            });
    });

    it('gets live item from ' + liveLocation, function (done) {
        request.get({url: liveLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });


});
