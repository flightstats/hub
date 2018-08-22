require('../integration_config');
const { getProp } = require('../lib/helpers');
const {
    getChannelUrl,
} = require('../lib/config');

const channelUrl = getChannelUrl();
const request = require('request');
const channelName = utils.randomChannelName();
const channelResource = `${channelUrl}/${channelName}`;

/**
 * This should verify that channel API endpoints return a 404 for missing items.
 */
describe(__filename, function () {
    function expect404 (url, done) {
        request.get({url: url, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(404);
                done();
            });
    }

    it('queries day', function (done) {
        expect404(`${channelResource}/2015/01/02`, done);
    });

    it('queries hour', function (done) {
        expect404(`${channelResource}/2015/01/02/03`, done);
    });

    it('queries minute', function (done) {
        expect404(`${channelResource}/2015/01/02/03/04`, done);
    });

    it('queries second', function (done) {
        expect404(`${channelResource}/2015/01/02/03/04/05`, done);
    });

    it('queries item', function (done) {
        expect404(`${channelResource}/2015/01/02/03/04/05/000/A`, done);
    });

    it('queries previous', function (done) {
        expect404(`${channelResource}/2015/01/02/03/04/05/000/A/previous`, done);
    });

    it('queries next', function (done) {
        expect404(`${channelResource}/2015/01/02/03/04/05/000/A/next`, done);
    });

    it('queries latest', function (done) {
        expect404(`${channelResource}/latest`, done);
    });

    it('queries latest 2', function (done) {
        expect404(`${channelResource}/latest/2`, done);
    });

    it('queries earliest', function (done) {
        expect404(`${channelResource}/earliest`, done);
    });

    it('queries earliest 2', function (done) {
        expect404(`${channelResource}/earliest/2`, done);
    });

    it('queries time', function (done) {
        expect404(`${channelResource}/time`, done);
    });

    it('queries time hour', function (done) {
        expect404(`${channelResource}/time/hour`, done);
    });

    it('queries time minute', function (done) {
        expect404(`${channelResource}/time/minute`, done);
    });

    it('queries status', function (done) {
        expect404(`${channelResource}/status`, done);
    });
});
