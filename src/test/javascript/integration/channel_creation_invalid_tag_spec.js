require('../integration_config');
const { getStatusCode } = require('../lib/helpers');


var channelName = utils.randomChannelName();

describe(__filename, function () {

    it('creates a channel with an invalid tag', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName, 'tags': ['foo bar', 'bar@home', 'tagz']};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getStatusCode(response)).toEqual(400);
            })
            .finally(done);
    });

});
