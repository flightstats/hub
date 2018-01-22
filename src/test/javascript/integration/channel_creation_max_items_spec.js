require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {
    
    it('verifies the channel doesn\'t exist yet', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });

    it('creates a channel with maxItems set', function (done) {
        var maxItems = 50;
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'maxItems': maxItems};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.headers['location']).toEqual(channelResource);
                expect(response.body._links.self.href).toEqual(channelResource);
                expect(response.body.name).toEqual(channelName);
                expect(response.body.maxItems).toEqual(maxItems);
                expect(response.body.description).toEqual('');
                expect(response.body.replicationSource).toEqual('');
            })
            .finally(done);
    });

    it('verifies the channel does exist', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
            })
            .finally(done);
    });
    
});
