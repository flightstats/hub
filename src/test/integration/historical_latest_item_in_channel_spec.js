require('./integration_config.js');

var request = require('request');
var channel = utils.randomChannelName();
var channelResource = channelUrl + "/" + channel;
var testName = __filename;
var moment = require('moment');


/**
 * create a channel
 * post 2 items
 * gets the item back out with latest
 * get both items back out with latest/10
 */
describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var items = [];

    it('posts two historical items', function (done) {
        var historicalItem1 = channelResource + '/' + '2016/11/20/12/00/00/000';
        var historicalItem2 = channelResource + '/' + '2016/11/20/12/01/00/000';
        utils.postItemQ(historicalItem1)
            .then(function (value) {
                items.push(value.response.headers.location);
                return utils.postItemQ(historicalItem2);
            })
            .then(function (value) {
                items.push(value.response.headers.location);
                done();
            });
    });

    it("gets latest in default Epoch in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?trace=true', 404, false, done);
    });

    it("gets latest Immutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets latest Mutable in channel ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=MUTABLE', 303, items[1], done);
    });

    var latest;

    it('posts item now', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                latest = value.response.headers.location;
                items.push(value.response.headers.location);
                done();
            });
    });

    it("gets latest in Immutable in channel - after now item", function (done) {
        utils.getLocation(channelResource + '/latest?stable=false', 303, latest, done);
    });

    it("gets latest Mutable in channel - after now item ", function (done) {
        utils.getLocation(channelResource + '/latest?epoch=MUTABLE&trace=true', 303, items[1], done);
    });

    it("gets latest N Mutable in channel ", function (done) {
        utils.getQuery(channelResource + '/latest/10?epoch=MUTABLE', 200, items.slice(0, 2), done);
    });

    it("gets latest N ALL in channel ", function (done) {
        utils.getQuery(channelResource + '/latest/10?stable=false&epoch=ALL', 200, items, done);
    });
});
