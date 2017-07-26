require('./integration_config.js');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

    it('fetches the list of channels', function (done) {
        utils.httpGet(channelUrl)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.body._links.self.href).toEqual(channelUrl);
                expect(response.body._links.channels).toContain(channelResource);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .fin(function () {
                done();
            });
    });

});
