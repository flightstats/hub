require('./integration_config.js');

var request = require('request');
var http = require('http');
var channel = utils.randomChannelName();
var moment = require('moment');
var channelResource = channelUrl + "/" + channel;
var testName = __filename;

describe(testName, function () {

    var mutableTime = moment.utc().subtract(1, 'year');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    var pointInThePast = '2014/06/01/12/00/00/000';
    var pointInThePastURL = channelResource + '/' + pointInThePast;

    it("adds item to " + pointInThePastURL, function (done) {
        request.post({
                url: pointInThePastURL,
                body: Array(41 * 1024 * 1024).join("a")
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(413);
                console.log('response location', response.headers.location);
                console.log('response body ', response.body);
                done();
            });
    }, 60099);

});

