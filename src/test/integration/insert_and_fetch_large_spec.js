require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;
var testName = __filename;

var MINUTE = 60 * 1000;

/**
 * 1 - create a large payload channel
 * 2 - post a large item (100+ MB)
 * 3 - fetch the item and verify bytes
 */
describe(testName, function () {

    var execute = true;

    it("checks the hub for large item suport", function (done) {
        request.get({
                url: hubUrlBase + '/internal/properties'
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parse = utils.parseJson(response, testName);
                console.log(response.body);
                var hubType = parse['properties']['hub.type'];
                execute = hubType == 'aws';
                console.log(hubType, 'execute', execute);
                done();
            });
    }, 5 * MINUTE);

    var channelBody = {
        tags: ["test"]
    };

    utils.putChannel(channelName, false, channelBody, testName);

    var items = [];
    var location;
    const SIZE = 41 * 1024 * 1024;

    it("posts a large item to " + channelResource, function (done) {
        if (execute) {
            request.post({
                    url: channelResource,
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
        } else {
            done();
        }
    }, 5 * MINUTE);

    it("gets item " + channelResource, function (done) {
        if (execute) {
            request.get({url: location},
                function (err, response, body) {
                    expect(err).toBeNull();
                    expect(response.statusCode).toBe(200);
                    expect(response.headers['content-type']).toBe('text/plain');
                    expect(response.body.length).toBe(SIZE - 1);
                    expect(parseInt(response.headers['x-item-length'])).toBe(SIZE - 1);
                    done();
                });
        } else {
            done();
        }
    }, 5 * MINUTE);

});
