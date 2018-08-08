require('../integration_config');

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
        utils.getLocation(channelResource + '/latest', 404, false, done);
    });

    it("gets latest stable Mutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=MUTABLE', 404, false, done);
    });

    it("gets latest 10 in default Epoch in channel ", function (done) {
        utils.getLocation(channelResource + '/latest/10?trace=true', 404, false, done);
    });

    it("gets latest 10 Immutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest/10?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets latest 10 Mutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest/10?epoch=MUTABLE', 404, false, done);
    });
});
