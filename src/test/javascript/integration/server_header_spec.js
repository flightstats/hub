require('../integration_config');
const request = require('request');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');
const { getHubUrlBase } = require('../lib/config');

const hubUrlBase = getHubUrlBase();

describe(__filename, function () {
    function verifyHeader (url, done) {
        console.log('url', url);
        request.get({
            url,
            headers: {"Content-Type": "application/json"},
        },
        function (err, response, body) {
            expect(err).toBeNull();
            expect(getProp('statusCode', response)).toBe(200);
            const server = fromObjectPath(['headers', 'server'], response) || '';
            expect(server.substring(0, 3)).toBe('Hub');
            done();
        });
    }

    it(`verfies header at root ${hubUrlBase}`, function (done) {
        verifyHeader(hubUrlBase, done);
    });

    it('verfies header at channel ', function (done) {
        verifyHeader(`${hubUrlBase}/channel`, done);
    });

    it('verfies header at health ', function (done) {
        verifyHeader(`${hubUrlBase}/health`, done);
    });
});
