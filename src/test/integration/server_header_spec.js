require('./integration_config.js');
var request = require('request');
var testName = __filename;

describe(testName, function () {

    function verifyHeader(url, done) {
        console.log('url', url);
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

    it('verfies header at root ' + hubUrlBase, function (done) {
        verifyHeader(hubUrlBase, done);
    });

    it('verfies header at channel ', function (done) {
        verifyHeader(hubUrlBase + '/channel', done);
    });

    it('verfies header at health ', function (done) {
        verifyHeader(hubUrlBase + '/health', done);
    });

});
