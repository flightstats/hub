require('../integration_config');
const { getProp } = require('../lib/helpers');
var request = require('request');
var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var testName = __filename;

/**
 * This should:
 *
 * 1 - create a channel
 * 2 - post items into the channel
 * 3 - verify that records are returned via time query
 */
describe(testName, function () {
    utils.createChannel(channelName);

    function expect400(url, done) {
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(400);
                console.log(getProp('body', response));
                done();
            });
    }

    it('queries item -1', function (done) {
        expect400(channelResource + '/2015/01/02/03/04/-1/000/a', done);
    });

    it('queries item 60', function (done) {
        expect400(channelResource + '/2015/01/02/03/04/60/000/a', done);
    });

    it('queries second -1', function (done) {
        expect400(channelResource + '/2015/01/02/03/04/-1', done);
    });

    it('queries second 60', function (done) {
        expect400(channelResource + '/2015/01/02/03/04/60', done);
    });

    it('queries minute -1', function (done) {
        expect400(channelResource + '/2015/01/02/03/-1', done);
    });

    it('queries minute 60', function (done) {
        expect400(channelResource + '/2015/01/02/03/60', done);
    });

    it('queries hour -1', function (done) {
        expect400(channelResource + '/2015/01/02/-1', done);
    });

    it('queries hour 24', function (done) {
        expect400(channelResource + '/2015/01/02/24', done);
    });

    it('queries day 0', function (done) {
        expect400(channelResource + '/2015/01/0', done);
    });

    it('queries day 32', function (done) {
        expect400(channelResource + '/2015/01/32', done);
    });

    it('queries month 0', function (done) {
        expect400(channelResource + '/2015/0/02', done);
    });

    it('queries month 13', function (done) {
        expect400(channelResource + '/2015/13/02', done);
    });

});
