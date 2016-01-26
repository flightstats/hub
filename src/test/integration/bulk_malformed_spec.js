require('./integration_config.js');

var request = require('request');
var Q = require('q');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * post malformed bulk payload
 * get back an error
 */
describe(testName, function () {

    utils.putChannel(channelName, false, {"name": channelName, "ttlDays": 1, "tags": ["bulk"]});

    var end_boundary = '--abcdefg--';

    var items = [];

    it("posts malformed item to " + channelResource, function (done) {
        request.post({
                url: channelResource + '/bulk',
                headers: {'Content-Type': "multipart/mixed; boundary=abcdefg"},
                body: end_boundary
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(400);
                console.log(response.body);
                done();
            });
    });

});
