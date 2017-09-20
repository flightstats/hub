require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('creates a channel with keepForever', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'keepForever': true};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(response.statusCode).toEqual(201);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.body.ttlDays).toEqual(0);
                expect(response.body.keepForever).toEqual(true);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .finally(done);
    });

    it('verifies the channel does exist', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                expect(response.statusCode).toEqual(200);
                expect(response.headers['content-type']).toEqual('application/json');
                expect(response.body.name).toEqual(channelName);
                expect(response.body.ttlDays).toEqual(0);
                expect(response.body.keepForever).toEqual(true);
            })
            .catch(function (error) {
                expect(error).toBeNull();
            })
            .finally(done);
    });

});
