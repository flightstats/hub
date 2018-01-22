require('../integration_config');

var channelName = utils.randomChannelName();

describe(__filename, function () {

    it('creates a channel with an invalid description', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'description': new Array(1026).join('a')};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(400);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
