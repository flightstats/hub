require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * create a channel
 * post an item
 * delete the item
 * get a 403
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

    it('deletes item', function (done) {
        request.del(posted,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(403);
                done();
            })
    })
});
