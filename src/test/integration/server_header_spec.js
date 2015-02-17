require('./integration_config.js');
var request = require('request');
var testName = __filename;

describe(testName, function () {

    var rootUrl = 'http://' + hubDomain + '/';

    function verifyHeader(url, done) {

        request.get({
                url: url,
                headers: {"Content-Type": "application/json"}
            },
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(response.headers.server.substring(0, 3)).toBe('Hub');
                done();
            });
    }

    it('verfies header at root ' + rootUrl, function (done) {
        verifyHeader(rootUrl, done);
    });

    it('verfies header at channel ', function (done) {
        verifyHeader(rootUrl + 'channel', done);
    });

    it('verfies header at health ', function (done) {
        verifyHeader(rootUrl + 'health', done);
    });

});
