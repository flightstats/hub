require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
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
 * todo Query items by next/previous, verify exclusion
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
                historicalLocation = value.response.headers.location;
                done();
            });
    });

    it('gets historical item from ' + historicalLocation, function (done) {
        request.get({url: historicalLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    });

    var liveLocation;

    it('posts live item to ' + channel, function (done) {
        utils.postItemQ(channelURL)
            .then(function (value) {
                liveLocation = value.response.headers.location;
                done();
            });
    });

    it('gets live item from ' + liveLocation, function (done) {
        request.get({url: liveLocation},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    });

    function timeQuery(query, expected, done) {
        request.get({url: channelURL + '/time/day?trace=true&stable=false' + query, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('body ', body);
                var uris = body._links.uris;
                console.log('uris ', uris);
                expect(uris.length).toBe(expected.length);
                for (var i = 0; i < uris.length; i++) {
                    expect(uris[i]).toBe(expected[i]);
                }
                done();
            });
    }

    it('gets default items from channel by day', function (done) {
        timeQuery('', [liveLocation], done);
    });

    it('gets immutable items from channel by day', function (done) {
        timeQuery('&epoch=IMMUTABLE', [liveLocation], done);
    });

    it('gets mutable items from channel by day', function (done) {
        timeQuery('&epoch=MUTABLE', [historicalLocation], done);
    });

    it('gets all items from channel by day', function (done) {
        timeQuery('&epoch=ALL', [historicalLocation, liveLocation], done);
    });

});
