require('../integration_config');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;
var testName = __filename;
var moment = require('moment');

var MINUTE = 60 * 1000;

/**
 * 1 - create a large payload channel
 * 2 - post a large item historical (100+ MB)
 * 3 - fetch the item and verify bytes
 */

describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channelName, false, channelBody, testName);

    var items = [];
    var location;
    const SIZE = 41 * 1024 * 1024;


    var pointInThePastURL = channelResource + '/' + mutableTime.format('YYYY/MM/DD/HH/mm/ss/SSS');
    var hashItem;
    it("posts a large historical item to " + channelName, function (done) {
        request.post({
                url: pointInThePastURL + '/large01',
                headers: {'Content-Type': "text/plain"},
                body: Array(SIZE).join("a")
            },
            function (err, response, body) {
                hashItem = response.headers.location;
                expect(err).toBeNull();
                done();
            });
    }, 5 * MINUTE);

    it("gets item " + channelName, function (done) {
        request.get({url: hashItem},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                done();
            });
    }, 5 * MINUTE);

});
