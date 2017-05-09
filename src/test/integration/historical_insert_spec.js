require('./integration_config.js');

var request = require('request');
var http = require('http');
var parse = require('parse-link-header');
var channel = utils.randomChannelName();
var moment = require('moment');

var testName = __filename;


/**
 * This should:
 * Create a channel with mutableTime
 * insert an item before the mutableTime, verify item with get
 * insert an item into now, verify item with get
 * insert an item with a user defined hash
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

    var hashItem;

    it('posts historical item to ' + channel, function (done) {
        utils.postItemQ(pointInThePastURL + '/abcdefg')
            .then(function (value) {
                hashItem = value.response.headers.location;
                expect(hashItem).toContain('/abcdefg');
                done();
            });
    });

    it('gets historical item from ' + hashItem, function (done) {
        request.get({url: hashItem},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    });

    // test large item in historical setting
    var items = [];
    var location;
    const SIZE = 41 * 1024 * 1024;
    var MINUTE = 60 * 1000;

    var payload = {
        url: channel,
        headers: {'Content-Type': "text/plain"},
        body: Array(SIZE).join("a")
    };
    var hashItem2;

    it("posts a large item to " + channel, function (done) {
        utils.postItemQwithPayload(pointInThePastURL + '/large01', payload)
            .then(function (value) {
                hashItem2 = value.response.headers.location;
                expect(hashItem).toContain('/large01');
                done();
            });
    }, 5 * MINUTE);

    it("gets item " + hashItem2, function (done) {
        request.get({url: hashItem2},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    }, 5 * MINUTE);

});
