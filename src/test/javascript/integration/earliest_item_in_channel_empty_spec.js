require('../integration_config');

var channelName = utils.randomChannelName();

describe(__filename, function () {

    var earliestURL;

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                earliestURL = response.body._links.earliest.href;
            })
            .finally(done);
    });

    it('verifies the earliest endpoint returns 404 on an empty channel', function (done) {
        utils.httpGet(earliestURL)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });

});
