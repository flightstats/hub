require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var request = require('request');
var testName = __filename;
var internalTimeUrl = hubUrlBase + '/internal/time'

/**
 * This should:
 *
 * 1 -
 */
describe(testName, function () {

    var links = {};
    var servers = [];
    var millis = Date.now() - 5;

    it('gets internal time links', function (done) {
        request.get({url: internalTimeUrl, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                console.log('body', body);
                links = getProp('_links', body) || {};
                servers = getProp('servers', body) || [];
                console.log('links', links);
                done();
            });
    });

    it('gets internal time', function (done) {
        request.get({url: links.local.href, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                expect(getProp('statusCode', response)).toBe(200);
                expect(body).toBeGreaterThan(millis);
                done();
            });
    });

    it('gets external time', function (done) {
        var expected = 200;
        if (servers.length === 1) {
            expected = 500;
        }
        const remoteLink = fromObjectPath(['remote', 'href'], links);
        request.get({url: remoteLink, json: true},
            function (err, response, body) {
                expect(err).toBeNull();
                const statusCode = getProp('statusCode', response);
                console.log('response.statusCode ' + statusCode);
                expect(statusCode).toBe(expected);
                if (statusCode === 200) {
                    console.log('body', body);
                    expect(body).toBeGreaterThan(millis);
                }
                done();
            });
    });

});
