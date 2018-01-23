require('../integration_config');

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

xdescribe(testName, function () {

    var callbackItems = [];
    var postedItems = [];

    utils.createChannel(channelName, false, testName);

    it('opens a stream', function (done) {
        var url = channelResource + '/stream';
        var headers = {"Content-Type": "application/json"};

        utils.httpGet(url, headers)
            .then(function (response) {
                expect(response.statusCode).toBe(200);
                console.log('body', body);
            })
            .finally(done);
    });

    it('inserts multiple items', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                return utils.postItemQ(channelResource);
            })
            .then(function (value) {
                postedItems.push(value);
                done();
            });
    });

    it('waits for the data', function (done) {
        utils.waitForData(callbackItems, postedItems, done);
    });

    it('verifies we got the correct number of items', function (done) {
        expect(callbackItems.length).toEqual(4);
    });

});
