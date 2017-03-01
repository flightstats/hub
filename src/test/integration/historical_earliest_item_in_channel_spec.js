require('./integration_config.js');

var request = require('request');
var channel = utils.randomChannelName();
var channelResource = channelUrl + "/" + channel;
var testName = __filename;
var moment = require('moment');


/**
 * create a channel
 * post 2 historical items
 * gets the item back out with earliest
 * post a current item
 * get items back out with earliest/10
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
        var historicalItem1 = channelResource + '/' + '2013/11/20/12/00/00/000';
        var historicalItem2 = channelResource + '/' + '2013/11/20/12/01/00/000';
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

    it("gets earliest in default Epoch in channel ", function (done) {
        utils.getLocation(channelResource + '/earliest?trace=true', 404, false, done);
    });

    it("gets earliest Immutable in channel missing ", function (done) {
        utils.getLocation(channelResource + '/earliest?epoch=IMMUTABLE', 404, false, done);
    });

    it("gets earliest Mutable in channel", function (done) {
        utils.getLocation(channelResource + '/earliest?epoch=MUTABLE', 303, items[0], done);
    });

    it('posts item now', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                items.push(value.response.headers.location);
                done();
            });
    });

    it("gets earliest Immutable in channel - after now item", function (done) {
        utils.getLocation(channelResource + '/earliest?stable=false&trace=true', 303, items[2], done);
    });

    it("gets earliest Mutable in channel - after now item", function (done) {
        utils.getLocation(channelResource + '/earliest?epoch=MUTABLE', 303, items[0], done);
    });

    it("gets earliest N Mutable in channel ", function (done) {
        utils.getQuery(channelResource + '/earliest/10?epoch=MUTABLE', 200, items.slice(0, 2), done);
    });

    it("gets earliest N ALL in channel ", function (done) {
        utils.getQuery(channelResource + '/earliest/10?stable=false&epoch=ALL', 200, items, done);
    });
});
