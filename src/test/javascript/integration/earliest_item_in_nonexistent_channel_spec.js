require('../integration_config');
const { getProp } = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

    it('verifies the earliest endpoint returns 404 on a nonexistent channel', function (done) {
        var url = channelResource + '/earliest';

        utils.httpGet(url)
            .then(function (response) {
                expect(getProp('statusCode', response)).toEqual(404);
            })
            .finally(done);
    });

});
