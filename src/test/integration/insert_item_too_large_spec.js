require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

describe(testName, function () {
    utils.createChannel(channelName);

    it("adds item to " + channelResource, function (done) {
        request.post({
                url: channelResource,
                body: Array(41 * 1024 * 1024).join("a")
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(413);
                console.log('posted', response.headers.location);
                done();
            });
    }, 60099);

});

