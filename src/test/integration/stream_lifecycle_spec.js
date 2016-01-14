require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - create a stream on that channel
 * 3 - post items into the channel
 * 4 - verify that the item payloads are returned within delta time
 */
describe(testName, function () {
    var callbackItems = [];
    var postedItems = [];

    utils.createChannel(channelName, false, testName);

    it('calls stream and waits for items', function (done) {

        request.get({
                url: channelResource + '/stream',
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('body', body);
                done();
            });

        utils.postItemQ(channelResource)
            .then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                return postedItem(value, true);
            }).then(function (value) {
                postedItem(value, false);
            });

        waitsFor(function () {
            return callbackItems.length == 4;
        }, 20 * 1000);


    });


});

