require('../integration_config');
const { getProp } = require('../lib/helpers');

describe(__filename, function () {

    it('creates a channel with an invalid name', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': 'not valid!'};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(400);
            })
            .finally(done);
    });

});
