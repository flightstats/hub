require('../integration_config');

var request = require('request');
var http = require('http');
var moment = require('moment');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.putChannel(channelName, function () {
    }, {"name": channelName, ttlDays: 1});

    it('gets latest ' + testName, function (done) {
        request.get({
                url: channelResource + '/latest?stable=false'
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    }, 2 * 60001);

});

