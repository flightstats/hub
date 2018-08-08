require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;
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
                const location = fromObjectPath(['response', 'headers', 'location'], value);
                posted = location;
                done();
            });
    });

    it("gets latest stable in channel ", function (done) {
        request.get({url: channelResource + '/latest', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });

    it("gets latest unstable in channel ", function (done) {
        request.get({url: channelResource + '/latest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(posted);
                done();
            });
    });

    it("gets latest N unstable in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const uris = fromObjectPath(['_links', 'uris'], parsed) || [];
                expect(uris.length).toBe(2);
                expect(uris[1]).toBe(posted);
                done();
            });
    });

    utils.itSleeps(6000);
    utils.addItem(channelResource, 201);

    it("gets latest stable in channel ", function (done) {
        request.get({url: channelResource + '/latest?stable=true&trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBe(posted);
                done();
            });
    });
});
