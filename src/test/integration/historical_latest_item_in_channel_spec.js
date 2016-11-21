require('./integration_config.js');

var request = require('request');
var channel = utils.randomChannelName();
var channelResource = channelUrl + "/" + channel;
var testName = __filename;
var moment = require('moment');


/**
 * create a channel
 * post 2 items
 * gets the item back out with latest
 * get both items back out with latest/10
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var historicalItem1 = channelResource + '/' + '2016/11/20/12/00/00/000';
    var historicalItem2 = channelResource + '/' + '2016/11/20/12/01/00/000';

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    utils.addItem(historicalItem1, 201);

    var historicalLatest;

    it('posts item', function (done) {
        utils.postItemQ(historicalItem2)
            .then(function (value) {
                historicalLatest = value.response.headers.location;
                done();
            });
    });

    function checkLatest(query, status, expected, done) {
        request.get({url: channelResource + query + '&trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(status);
                if (expected) {
                    expect(response.headers.location).toBe(expected);
                }
                done();
            });
    }

    it("gets latest in default Epoch in channel ", function (done) {
        checkLatest('/latest?trace=true', 404, false, done);
    });

    it("gets latest Immutable in channel ", function (done) {
        checkLatest('/latest?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets latest Mutable in channel ", function (done) {
        checkLatest('/latest?epoch=MUTABLE', 303, historicalLatest, done);
    });

    var latest;

    it('posts item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                latest = value.response.headers.location;
                done();
            });
    });

    it("gets latest in Immutable in channel ", function (done) {
        checkLatest('/latest?stable=false', 303, latest, done);
    });

    it("gets latest Mutable in channel ", function (done) {
        checkLatest('/latest?epoch=MUTABLE', 303, historicalLatest, done);
    });

    it("gets latest N Mutable in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?trace=true&epoch=MUTABLE', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, testName);
                if (parsed._links) {
                    expect(parsed._links.uris.length).toBe(2);
                    expect(parsed._links.uris[1]).toBe(historicalLatest);
                }
                done();
            });
    });

    it("gets latest N ALL in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?stable=false&epoch=ALL', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, testName);
                if (parsed._links) {
                    expect(parsed._links.uris.length).toBe(3);
                    expect(parsed._links.uris[1]).toBe(historicalLatest);
                    expect(parsed._links.uris[2]).toBe(latest);
                }
                done();
            });
    });
});
