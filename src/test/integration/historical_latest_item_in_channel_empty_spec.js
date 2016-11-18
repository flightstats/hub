require('./integration_config.js');

var request = require('request');
var channel = utils.randomChannelName();
var channelResource = channelUrl + "/" + channel;
var testName = __filename;
var moment = require('moment');

/**
 * create a channel
 * get latest returns 404
 * get latest/10 returns 404
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    it("gets latest stable in channel ", function (done) {
        request.get({url: channelResource + '/latest', followRedirect: false},
            function (err, response, body) {
                expect(response.statusCode).toBe(404);
                done();
            });
    });

    it("gets latest N in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });
});
