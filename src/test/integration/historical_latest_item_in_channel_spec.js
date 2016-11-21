require('./integration_config.js');

var request = require('request');
var channel = utils.randomChannelName();
var channelResource = channelUrl + "/" + channel;
var testName = __filename;
var moment = require('moment');


/**
 * create a channel
 * post 2 items
 * gets the item back out with latest
 * get both items back out with latest/10
 */
describe(testName, function () {

    var historicalItem1 = channelResource + '/' + '2014/06/01/12/00/00/000';
    var historicalItem2 = channelResource + '/' + '2014/06/01/12/01/00/000';

    var mutableTime = moment.utc().subtract(1, 'minute');

    var channelBody = {
        mutableTime: mutableTime.format('YYYY-MM-DDTHH:mm:ss.SSS'),
        tags: ["test"]
    };

    utils.putChannel(channel, false, channelBody, testName);

    utils.addItem(historicalItem1, 201);

    var posted;

    it('posts item', function (done) {
        utils.postItemQ(historicalItem2)
            .then(function (value) {
                posted = value.response.headers.location;
                done();
            });
    });

    /*    it("gets latest Immutable in channel ", function (done) {
     request.get({url: channelResource + '/latest?trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
     expect(response.statusCode).toBe(404);
     done();
     });
     });*/

    it("gets latest Mutable in channel ", function (done) {
        request.get({url: channelResource + '/latest?trace=true&epoch=MUTABLE', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(303);
                expect(response.headers.location).toBe(posted);
                done();
            });
    });

    //todo gfm - also add a new item, check latest varieties

    //todo gfm - test latest N which could go back to 1970...

    /*it("gets latest N stable in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                var parsed = utils.parseJson(response, testName);
                if (parsed._links) {
                    expect(parsed._links.uris.length).toBe(2);
                    expect(parsed._links.uris[1]).toBe(posted);
                }
                done();
            });
     });*/
});
