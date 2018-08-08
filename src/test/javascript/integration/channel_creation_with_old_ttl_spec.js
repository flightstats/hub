require('../integration_config');
const { getProp } = require('../lib/helpers');

var channelName = utils.randomChannelName();

describe(__filename, function () {
    it('creates a channel with an old TTL', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'ttlMillis': 0};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
            })
            .finally(done);
    });
});
