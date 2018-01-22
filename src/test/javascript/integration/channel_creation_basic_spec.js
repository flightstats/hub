require('../integration_config');

const channelName = utils.randomChannelName();
const channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('verifies the channel doesn\'t exist', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });

    it('creates the channel', function (done) {
        var uri = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(uri, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.headers['location']).toEqual(channelResource);
                expect(response.body._links.self.href).toEqual(channelResource);
                expect(response.body.name).toEqual(channelName);
                expect(response.body.ttlDays).toEqual(120);
                expect(response.body.description).toEqual('');
                expect(response.body.replicationSource).toEqual('');
                expect(response.body.storage).toEqual('SINGLE');
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
