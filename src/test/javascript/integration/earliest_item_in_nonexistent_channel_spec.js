require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

    it('verifies the earliest endpoint returns 404 on a nonexistent channel', function (done) {
        var url = channelResource + '/earliest';

        utils.httpGet(url)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .finally(done);
    });

});
