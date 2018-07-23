require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * create a channel
 * post two items
 * stream both items back with batch
 */
describe(testName, function () {

    utils.putChannel(channelName, function () {
    }, {"name": channelName, "ttlDays": 1});

    utils.addItem(channelResource, 201);

    it('posts item', function (done) {
        utils.postItemQ(channelResource)
            .then(function (value) {
                const location = fromObjectPath(['response', 'headers', 'location'], value);
                console.log('location: ', location);
                done();
            });
    });

    it("gets zip items ", function (done) {
        request.get({
            url: channelResource + '/latest/10?stable=false&batch=true',
            followRedirect: false,
            headers: {Accept: "application/zip"}
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            // todo - gfm - 8/19/15 - parse zip
            console.log("headers", getProp('headers', response));
            console.log("body", getProp('body', response));
            done();
        });
    });
});
