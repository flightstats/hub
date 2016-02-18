require('./integration_config.js');

var request = require('request');
var moment = require('moment');
var testName = __filename;
var internalTimeUrl = hubUrlBase + '/internal/time'

/**
 * This should:
 *
 * 1 -
 */
describe(testName, function () {

    var links = {};
    var millis = Date.now() - 5;

    it('gets internal time links', function (done) {
        request.get({url: internalTimeUrl, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('body', body);
                links = body._links;
                console.log('links', links);
                done();
            })
    });

    it('gets internal time', function (done) {
        request.get({url: links.local.href, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                expect(body).toBeGreaterThan(millis);
                done();
            })
    });

    it('gets external time', function (done) {
        request.get({url: links.remote.href, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(response.statusCode).toBe(200);
                console.log('body', body);
                expect(body).toBeGreaterThan(millis);
                done();
            })
    });

});

