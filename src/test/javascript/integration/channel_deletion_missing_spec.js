require('../integration_config');

var channelResource = channelUrl + '/nonExistent';

describe(__filename, function () {

    it('deletes a channel that doesn\'t exist', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'application/json'};

        utils.httpDelete(url, headers)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });

});
