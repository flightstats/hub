require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var messageText = "MY SUPER TEST CASE: this & <that>. " + Math.random().toString();

describe(__filename, function () {

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    var itemURL;

    it('inserts and item', function (done) {
        var url = channelResource;
        var headers = {};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.body._links.channel.href).toEqual(channelResource);
                itemURL = response.body._links.self.href;
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('verifies the correct Content-Type header is returned', function (done) {
        utils.httpGet(itemURL)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['content-type']).toEqual('application/octet-stream');
                expect(response.body).toContain(messageText);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
