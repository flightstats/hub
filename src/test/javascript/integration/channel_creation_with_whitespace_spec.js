require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

    it('creates a channel with whitespace in the name', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': '    ' + channelName + '    '};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.headers['location']).toEqual(channelResource);
            })
            .finally(done);
    });

});
