require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * post two items
 * stream both items back with batch
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


    it("gets zip items ", function (done) {
        request.get({
                url: channelResource + '/latest/10?stable=false&batch=true',
                followRedirect: false,
                headers: {Accept: "application/zip"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                //todo - gfm - 8/19/15 - parse zip
                console.log("headers", response.headers);
                console.log("body", response.body);
                done();
            });
    });
});
