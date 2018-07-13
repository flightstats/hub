require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

describe(__filename, function () {

    it('verifies the health check returns healthy', function (done) {
        var url = hubUrlBase + '/health';

        utils.httpGet(url)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const healthy = fromObjectPath(['body', 'healthy'], response);
                expect(getProp('statusCode', response)).toEqual(200);
                expect(contentType).toEqual('application/json');
                expect(healthy).toEqual(true);
            })
            .finally(done);
    });

});
