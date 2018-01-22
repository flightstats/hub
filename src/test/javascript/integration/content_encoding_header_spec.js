require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var messageText = "Testing that the Content-Encoding header is returned";

describe(__filename, function () {

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .finally(done);
    });

    it('inserts an item', function (done) {
        var url = channelResource;
        var headers = {};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .finally(done);
    });

    it('verifies the Content-Encoding header is returned', function (done) {
        var url = channelResource;
        var headers = {'accept-encoding': 'gzip'};

        utils.httpGet(url, headers)
            .then(function (response) {
                expect(response.headers['content-encoding']).toEqual('gzip');
            })
            .finally(done);
    });

});
