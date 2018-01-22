require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + "/" + channelName;

describe(__filename, function () {

    it('verifies a 404 is returned on a nonexistent channel', function (done) {
        var url = channelResource + '/2014/12/31/23/59/59/999/685221b0-77c2-11e2-8a3e-20c9d08600a5';

        utils.httpGet(url)
            .then(function (response) {
                expect(response.statusCode).toEqual(404);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
