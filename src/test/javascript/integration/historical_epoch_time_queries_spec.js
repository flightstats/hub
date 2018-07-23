require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var channel = utils.randomChannelName();
var moment = require('moment');

var tag = Math.random().toString().replace(".", "");
var testName = __filename;

/**
 * This should:
 * Create a channel with mutableTime
 * insert an item before the mutableTime, verify item with get
 * insert an item into now, verify item with get
 * Query items by time, verify exclusion
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: [tag, "test"]
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

    it('gets historical item from ' + historicalLocation, function (done) {
        request.get({url: historicalLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                done();
            });
    });

    var liveLocation;
    var liveTime;

    it('posts live item to ' + channel, function (done) {
        utils.postItemQ(channelURL)
            .then(function (value) {
                liveLocation = fromObjectPath(['response', 'headers', 'location'], value);
                liveTime = moment((liveLocation || '').substring(channelURL.length), '/YYYY/MM/DD/HH/mm/ss/SSS');
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

    function timeQuery(query, expected, done) {
        var url = channelURL + query;
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const uris = fromObjectPath(['_links', 'uris'], body) || [];
                expect(uris.length).toBe(expected.length);
                for (var i = 0; i < uris.length; i++) {
                    expect(expected[i]).toBeDefined();
                    expect(uris[i]).toBe(expected[i]);
                }
                done();
            });
    }

    function queryTimes(format, done) {
        var liveQuery = liveTime && liveTime.format(format);
        var mutableQuery = mutableTime && mutableTime.format(format);

        var queryAll = done;
        if (liveQuery && (liveQuery === mutableQuery)) {
            queryAll = function () {
                timeQuery(mutableQuery + '?epoch=ALL&trace=true&stable=false', [historicalLocation, liveLocation], done);
            };
        }

        var queryMutable = function () {
            timeQuery(mutableQuery + '?epoch=MUTABLE&trace=true&stable=false', [historicalLocation], queryAll);
        };
        timeQuery(liveQuery + '?epoch=IMMUTABLE&trace=true&stable=false', [liveLocation], queryMutable);
    }

    it('mutable item by day', function (done) {
        queryTimes('/YYYY/MM/DD', done);
    });

    it('mutable item by hour', function (done) {
        queryTimes('/YYYY/MM/DD/HH', done);
    });

    it('mutable item by minute', function (done) {
        queryTimes('/YYYY/MM/DD/HH/mm', done);
    });

    it('mutable item by second', function (done) {
        queryTimes('/YYYY/MM/DD/HH/mm/ss', done);
    });

    it('mutable item by millis', function (done) {
        queryTimes('/YYYY/MM/DD/HH/mm/ss/SSS', done);
    });

});
