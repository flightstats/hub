require('../integration_config');
const { getProp } = require('../lib/helpers');

var channelName = utils.randomChannelName();

describe(__filename, function () {
    it('creates a channel with an invalid description', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'description': new Array(1026).join('a')};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(400);
            })
            .finally(done);
    });
});
