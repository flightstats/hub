require('./integration_config.js');

var request = require('request');
var channel = utils.randomChannelName();
var channelResource = channelUrl + "/" + channel;
var testName = __filename;
var moment = require('moment');

/**
 * create a channel
 * get earliest returns 404
 * get earliest/10 returns 404
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    it("gets earliest in default Epoch in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest?trace=true', 404, false, done);
    });

    it("gets earliest Immutable in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets earliest Mutable in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest?epoch=MUTABLE', 404, false, done);
    });

    it("gets earliest 10 in default Epoch in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest/10?trace=true', 404, false, done);
    });

    it("gets earliest 10 Immutable in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest/10?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets earliest 10 Mutable in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest/10?epoch=MUTABLE', 404, false, done);
    });

});
