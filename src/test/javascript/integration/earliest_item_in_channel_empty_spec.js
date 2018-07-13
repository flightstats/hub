require('../integration_config');
const {
    fromObjectPath,
    getProp,
} = require('../lib/helpers');

var channelName = utils.randomChannelName();

describe(__filename, function () {

    var earliestURL;

    it('creates a channel', function (done) {
        var url = channelUrl;
        var headers = {'Content-Type': 'application/json'};
        var body = {'name': channelName};

        utils.httpPost(url, headers, body)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(201);
                earliestURL = fromObjectPath(['body', '_links', 'earliest', 'href'], response);
            })
            .finally(done);
    });

    it('verifies the earliest endpoint returns 404 on an empty channel', function (done) {
        if (!earliestURL) return done.fail('required earliestURL not defined by last test');
        utils.httpGet(earliestURL)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(404);
            })
            .finally(done);
    });

});
