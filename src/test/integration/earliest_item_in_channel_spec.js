require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * post an item
 * does not get the item back out with earliest - stable
 * get the item back out with earliest - unstable
 */
describe(testName, function () {

    utils.putChannel(channelName, function () {
    }, {"name": channelName, "ttlDays": 1});

    var posted;

    it('posts item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                posted = value.response.headers.location;
                done();
            });
    });

    utils.addItem(channelResource, 201);

    it("gets earliest stable in channel ", function (done) {
        request.get({url: channelResource + '/earliest', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });

    it("gets earliest unstable in channel ", function (done) {
        request.get({url: channelResource + '/earliest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(posted);
                done();
            });
    });

    it("gets earliest N unstable in channel ", function (done) {
        request.get({url: channelResource + '/earliest/10?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, testName);
                expect(parsed._links.uris.length).toBe(2);
                expect(parsed._links.uris[0]).toBe(posted);
                expect(parsed._links.next).toBeDefined();
                expect(parsed._links.previous).not.toBeDefined();
                done();
            });
    });
});
