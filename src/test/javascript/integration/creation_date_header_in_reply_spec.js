require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;
var messageText = "there's a snake in my boot!";

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

    var itemURL;

    it('inserts an item', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'text/plain'};
        var body = messageText;

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                itemURL = response.body._links.self.href;
            })
            .finally(done);
    });

    it('verifies the creation-date header is returned', function (done) {
        utils.httpGet(itemURL)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['creation-date']).toContain('T');
            })
            .finally(done);
    });

});
