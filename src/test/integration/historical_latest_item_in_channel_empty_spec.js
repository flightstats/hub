require('./integration_config.js');

var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
utils.configureFrisby();


/**
 * create a channel
 * post 2 items
 * gets the item back out with latest
 * get both items back out with latest/10
 */
describe(testName, function () {

    var historicalItem1 = channelResource + '/' + '2014/06/01/12/00/00/000';
    var historicalItem2 = channelResource + '/' + '2014/06/01/12/01/00/000';

    utils.putChannel(channelName, function () {
    }, {"name": channelName, "ttlDays": 10000, historical: true});

    it("gets latest stable in channel ", function (done) {
        request.get({url: channelResource + '/latest', followRedirect: false},
            function (err, response, body) {
                expect(response.statusCode).toBe(404);
                done();
            });
    });

    it("gets latest N in channel ", function (done) {
        request.get({url: channelResource + '/latest/10?trace=true', followRedirect: false},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            });
    });
});
