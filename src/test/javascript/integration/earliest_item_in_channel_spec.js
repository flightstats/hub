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
                posted = fromObjectPath(['response', 'headers', 'location'], value);
                done();
            });
    });

    utils.addItem(channelResource, 201);

    it("gets earliest stable in channel ", function (done) {
        request.get({url: channelResource + '/earliest', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    });

    it("gets earliest unstable in channel ", function (done) {
        request.get({url: channelResource + '/earliest?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(303);
                const location = fromObjectPath(['headers', 'location'], response);
                expect(location).toBeDefined();
                expect(posted).toBeDefined();
                expect(location).toBe(posted);
                done();
            });
    });

    it("gets earliest N unstable in channel ", function (done) {
        request.get({url: channelResource + '/earliest/10?stable=false', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                var parsed = utils.parseJson(response, testName);
                const links = getProp('_links', parsed);
                if (links) {
                    const { next, previous, uris = [] } = links;
                    expect(uris.length).toBe(2);
                    expect(posted).toBeDefined();
                    expect(uris[0]).toBe(posted);
                    expect(next).toBeDefined();
                    expect(previous).not.toBeDefined();
                } else {
                    expect(links).toBe(true);
                }
                done();
            });
    });
});
