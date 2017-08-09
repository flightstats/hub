require('../integration_config');

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
    var returnedBody;

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;

        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.allowZeroBytes).toEqual(true);
    }, {});

    it("adds zero byte item", function (done) {
        request.post({
                url: channelResource,
                headers: {"Content-Type": "text/plain"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(201);
                console.log('posted', response.headers.location);
                done();
            });
    });

    utils.putChannel(channelName, function (response, body) {
        var parse = utils.parseJson(response, testName);
        returnedBody = parse;

        expect(parse._links.self.href).toEqual(channelResource);
        expect(parse.allowZeroBytes).toEqual(false);
    }, {allowZeroBytes: false});

    utils.itRefreshesChannels();

    it("fails to add zero byte item", function (done) {
        request.post({
                url: channelResource,
                headers: {"Content-Type": "text/plain"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(400);
                console.log('posted', response.headers.location);
                done();
            });
    });

});


