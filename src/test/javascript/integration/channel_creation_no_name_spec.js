require('../integration_config');

describe(__filename, function () {

    it('creates a channel with no name', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(400);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
