require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * post an item
 * does get the item back out with latest - stable
 * get the item back out with latest - unstable
 */
describe(testName, function () {

    utils.putChannel(channelName);

    utils.addItem(channelResource, 201);

    var posted;

    it('posts item', function () {
        utils.postItemQ(channelResource)
            .then(function (value) {
                posted = value.response.headers.location;
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

});
