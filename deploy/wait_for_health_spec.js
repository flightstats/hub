//var host = process.env.host || 'hub-02.cloud-east.dev:8080';
var host = process.env.host || 'localhost';

var request = require('request');
var http = require('http');
var testName = __filename;

/**
 * This should:
 *
 * wait for the health check to come up
 */
describe(testName, function () {

    var url = 'http://' + host + ':8080/health';
    console.log(url);

    function healthCheckTimeout(done) {
        setTimeout(function () {
            healthCheck(done);
        }, 5 * 1000);
    }

    function healthCheck(done) {
        request.get({url : url },
            function (err, response, body) {
                if (err !== null) {
                    healthCheckTimeout(done);
                } else if (response.statusCode !== 200) {
                    console.log('response.statusCode', response.statusCode);
                    healthCheckTimeout(done);
                } else if (response.statusCode === 200) {
                    console.log(body);
                    var parse = JSON.parse(body);
                    expect(parse.healthy).toBe(true);
                    done();
                }

            });
    }

    it('waits for health check', function (done) {
        healthCheck(done)
    }, 3 * 60 * 1000);

});

