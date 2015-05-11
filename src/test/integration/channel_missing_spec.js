require('./integration_config.js');

var request = require('request');
var http = require('http');
var channelName = utils.randomChannelName();
var groupName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;
var port = callbackPort + 2;
var callbackUrl = callbackDomain + ':' + port + '/';
var groupConfig = {
    callbackUrl: callbackUrl,
    channelUrl: channelResource
};

/**
 * This should verify that channel API endpoints return a 404 for missing items.
 */
describe(testName, function () {

    function expect404(url, done) {
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(404);
                done();
            })
    }

    it('queries day', function (done) {
        expect404(channelResource + '/2015/01/02', done);
    });

    it('queries hour', function (done) {
        expect404(channelResource + '/2015/01/02/03', done);
    });

    it('queries minute', function (done) {
        expect404(channelResource + '/2015/01/02/03/04', done);
    });

    it('queries second', function (done) {
        expect404(channelResource + '/2015/01/02/03/04/05', done);
    });

    it('queries item', function (done) {
        expect404(channelResource + '/2015/01/02/03/04/05/000/A', done);
    });

    it('queries previous', function (done) {
        expect404(channelResource + '/2015/01/02/03/04/05/000/A/previous', done);
    });

    it('queries next', function (done) {
        expect404(channelResource + '/2015/01/02/03/04/05/000/A/next', done);
    });

    it('queries latest', function (done) {
        expect404(channelResource + '/latest', done);
    });

    it('queries latest 2', function (done) {
        expect404(channelResource + '/latest/2', done);
    });

    it('queries earliest', function (done) {
        expect404(channelResource + '/earliest', done);
    });

    it('queries earliest 2', function (done) {
        expect404(channelResource + '/earliest/2', done);
    });

});

