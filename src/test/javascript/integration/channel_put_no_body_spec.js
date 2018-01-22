require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('creates a channel with no information', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'application/json'};
        var body = '';

        utils.httpPut(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .finally(done);
    });

});
