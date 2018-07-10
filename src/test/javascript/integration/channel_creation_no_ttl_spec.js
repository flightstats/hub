require('../integration_config');
const {
  fromObjectPath,
  getStatusCode,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('verifies the channel doesn\'t exist yet', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                expect(getStatusCode(response)).toEqual(404);
            })
            .finally(done);
    });

    it('creates a channel with no TTL', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'ttlMillis': null};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getStatusCode(response)).toEqual(201);
            })
            .finally(done);
    });

    it('verifies the channel does exist', function (done) {
        utils.httpGet(channelResource)
            .then(function (response) {
                const contentType = fromObjectPath(['headers', 'content-type'], response);
                const name = fromObjectPath(['body', 'name'], response);
                expect(getStatusCode(response)).toEqual(200);
                expect(contentType).toEqual('application/json');
                expect(name).toEqual(channelName);
            })
            .finally(done);
    });

});
