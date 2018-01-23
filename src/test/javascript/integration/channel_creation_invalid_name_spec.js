require('../integration_config');

describe(__filename, function () {

    it('creates a channel with an invalid name', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': 'not valid!'};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(400);
            })
            .finally(done);
    });

});
