require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;
var testName = __filename;

var MINUTE = 60 * 1000;

/**
 * 1 - create a channel
 * 2 - post an item with threads
 * 3 - fetch the item and verify bytes
 */
describe(testName, function () {

    var channelBody = {
        tags: ["test"]
    };

    utils.putChannel(channelName, false, channelBody, testName);

    var location;
    const SIZE = 41 * 1024;

    it("posts a large item to " + channelResource, function (done) {
        request.post({
                url: channelResource + '?threads=3',
                headers: {'Content-Type': "text/plain"},
                body: Array(SIZE).join("a")
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                var parse = utils.parseJson(response, testName);
                console.log(response.body);
                location = response.headers.location;
                console.log(location);
                done();
            });

    }, 5 * MINUTE);

    it("gets item " + channelResource, function (done) {
        request.get({url: location},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(response.headers['content-type']).toBe('text/plain');
                expect(response.body.length).toBe(SIZE - 1);
                done();
            });
    }, 5 * MINUTE);

});
