require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
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

    var channelBody = {
        tags: ["test"]
    };

    utils.putChannel(channelName, false, channelBody, testName);

    var location;
    const SIZE = 41 * 1024 * 1024;

    it("posts a large item to " + channelResource, function (done) {
        request.post({
            url: channelResource,
            headers: {'Content-Type': "text/plain"},
            body: Array(SIZE).join("a")
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(201);
            console.log(getProp('body', response));
            location = fromObjectPath(['headers', 'location'], response);
            console.log(location);
            done();
        });
    }, 5 * MINUTE);

    it("gets item " + channelResource, function (done) {
        request.get({url: location},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const xItemLength = fromObjectPath(['headers', 'x-item-length'], response);
                const responseBody = getProp('body', response) || '';
                expect(contentType).toBe('text/plain');
                expect(responseBody.length).toBe(SIZE - 1);
                expect(parseInt(xItemLength, 10)).toBe(SIZE - 1);
                done();
            });
    }, 5 * MINUTE);

});
