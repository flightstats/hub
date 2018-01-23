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

    it('creates the channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'ttlMillis': null};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
            })
            .finally(done);
    });

    it('updates the channel TTL', function (done) {
        var url = channelResource;
        var headers = {'Content-Type': 'application/json'};
        var body = {'ttlMillis': null};
        
        utils.httpPatch(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.body.name).toEqual(channelName);
            })
            .finally(done);
    });
    
});
