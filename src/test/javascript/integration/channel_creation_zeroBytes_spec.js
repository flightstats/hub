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
 * create a channel via put with allowZeroBytes == false
 * verify that we can not post a zero byte item.
 * change the property to true.
 * Post a zero byte item.
 *
 */
describe(testName, function () {
    var returnedBody; // TODO: is this actually being used?
    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;
        const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
        const allowZeroBytes = getProp('allowZeroBytes', parse);
        expect(selfLink).toEqual(channelResource);
        expect(allowZeroBytes).toEqual(true);
    }, {});

    it("adds zero byte item", function (done) {
        request.post({
            url: channelResource,
            headers: {"Content-Type": "text/plain"},
        },
        function (err, response, body) {
            expect(err).toBeNull();
            const location = fromObjectPath(['headers', 'location'], response);
            expect(getProp('statusCode', response)).toBe(201);
            console.log('posted', location);
            done();
        });
    });

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;
        const selfLink = fromObjectPath(['_links', 'self', 'href'], parse);
        const allowZeroBytes = getProp('allowZeroBytes', parse);
        expect(selfLink).toEqual(channelResource);
        expect(allowZeroBytes).toEqual(false);
    }, {allowZeroBytes: false});

    utils.itRefreshesChannels();

    it("fails to add zero byte item", function (done) {
        request.post({
            url: channelResource,
            headers: {"Content-Type": "text/plain"},
        },
        function (err, response, body) {
            expect(err).toBeNull();
            const location = fromObjectPath(['headers', 'location'], response);
            expect(getProp('statusCode', response)).toBe(400);
            console.log('posted', location);
            done();
        });
    });
});
