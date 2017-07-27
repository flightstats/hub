require('./integration_config.js');

describe(__filename, function () {

    it('verifies the health check returns healthy', function (done) {
        var url = hubUrlBase + '/health';

        utils.httpGet(url)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.body.healthy).toEqual(true);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

});
