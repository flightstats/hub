require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * call latest in spoke on the empty channel
 */
describe(testName, function () {

    utils.putChannel(channelName);

    it("calls spoke " + channelName + " at " + channelUrl, function (done) {
        var url = hubUrlBase + '/internal/spoke/latest/' + channelName + "/2016/03/17/18/09/42/000/~ZZZZZZ";
        console.log("putting channel at " + url);
        request.get(url,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                console.log("respinse " + body)
                done();
            });
    });

});
