require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;


/**
 * create a channel
 * post an item
 * does not get the item back out with latest - stable
 * get the item back out with latest - unstable
 */
describe(testName, function () {

    utils.putChannel(channelName, function () {
    }, {"name": channelName, "ttlDays": 1});

    utils.addItem(channelResource, 201);

    var posted;

    it('posts item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                posted = value.response.headers.location;
                done();
            });
    });

    it("gets latest stable in channel ", function (done) {
        request.get({url: channelResource + '/latest', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });

    it("gets latest unstable in channel ", function (done) {
        request.get({url: channelResource + '/latest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(posted);
                done();
            });
    });

    it("gets latest N unstable in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, testName);
                if (parsed._links) {
                    expect(parsed._links.uris.length).toBe(2);
                    expect(parsed._links.uris[1]).toBe(posted);
                }
                done();
            });
    });

    utils.sleep(6000);
    utils.addItem(channelResource, 201);

    it("gets latest stable in channel ", function (done) {
        request.get({url: channelResource + '/latest?stable=true&trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(posted);
                done();
            });
    });
});
