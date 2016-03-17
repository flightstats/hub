require('./integration_config.js');

var request = require('request');
var moment = require('moment');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * post an item
 * call latest in spoke before the item
 * call latest in spoke at the item
 * call latest in spoke after the item
 */
describe(testName, function () {

    utils.putChannel(channelName);

    var posted;
    var item;

    var spokeForChannel = hubUrlBase + '/internal/spoke/latest/' + channelName;
    var start = moment().utc().subtract(1, 'minute');

    it('posts item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                posted = value.response.headers.location;
                item = posted.substring(posted.indexOf(channelName) + channelName.length, posted.length);
                console.log('posted', posted);
                done();
            });
    });

    it("calls spoke before item " + channelName, function (done) {
        var url = spokeForChannel + start.format('/YYYY/MM/DD/HH/mm/ss/SSS') + "/~ZZZZZZ";
        console.log("calling spoke channel at " + url);
        request.get(url,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                console.log("respinse " + body)
                done();
            });
    });

    it("calls spoke at item " + channelName, function (done) {
        var url = spokeForChannel + item;
        request.get(url,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                console.log("respinse " + body)
                done();
            });
    });

    it("calls spoke after item " + channelName, function (done) {
        var after = start.add(2, 'minute');
        var url = spokeForChannel + after.format('/YYYY/MM/DD/HH/mm/ss/SSS') + "/~ZZZZZZ";
        request.get(url,
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                //test_0_6179123660549521/2016/03/17/18/39/00/687/LKHKbM
                console.log("respinse " + body)
                expect(body).toBe(channelName + item);
                done();
            });
    });

});
